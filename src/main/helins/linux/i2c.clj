;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.linux.i2c

  "The Linux kernel provides a standard interface for performing I2C operations.
  
   This library exposes this interface in a clojure idiomatic way.

   Each IO operation might throw if something fails.

   Essentially, IO can be performed by directly reading and writing arbitrary bytes, doing
   transactions (uninterrupted sequence of messages) and using standard SMBus operations.

   Not everything is supported by your driver, refer to [[]]. Furthermore, slave devices
   are often buggy and imperfect."

  {:author "Adam Helinski"}

  (:import (io.helins.linux.i2c I2CBuffer
                                I2CBus
                                I2CMessage
                                I2CFlag
                                I2CFlags
                                I2CFunctionality
                                I2CTransaction)
           java.lang.AutoCloseable)
  (:refer-clojure :exclude [read]))


;;;;;;;;;; Default values


(def default+

  "Defaults values for options used throughout this library."

  {:i2c/bit-10?        false
   :i2c/force?         false
   :i2c/ignore-nak?    false
   :i2c/no-read-ack?   false
   :i2c/no-start?      false
   :i2c/revise-rw-bit? false
   :i2c/slave-address  0})



(defn- -obtain

  ;;

  [k hmap]

  (or (get hmap
           k)
      (get default+ k)))


;;;;;;;;;;


(defn bus

  "Opens an I2C bus by providing the number of the bus or a direct path.
  
   ```clojure
   (with-open [my-bus (bus \"/dev/i2c-1\")]
     ...)
   ```"

  ^AutoCloseable

  [bus-path]

  (if (string? bus-path)
    (I2CBus. ^String bus-path)
    (I2CBus. ^int    bus-path)))




(defn close

  "Closes an I2C bus."

  [^I2CBus bus]

  (.close bus)
  nil)




(defn capability+

  "Retrieves the I2C of the given bus.

   Not every driver is capable of doing everything this library offers, specially when it comes to SMBus operations.

   Even then, support can be unperfect. For instance, sometimes transactions are supported but fail when they contain
   more than 1 message, which makes them quite useless.

   Furthermore, a lot also depends on the slave device.
  
   Functions from this library document what need to be checked."

  [^I2CBus bus]

  (let [functionalities (.getFunctionalities bus)]
    (into #{}
          (comp (filter (fn only-ok [functionality]
                          (.can functionalities
                                functionality)))
                (map (fn convert [functionality]
                       (condp identical?
                              functionality
                         I2CFunctionality/BLOCK_PROCESS_CALL  :block-process-call
                         I2CFunctionality/PROCESS_CALL        :process-call
                         I2CFunctionality/PROTOCOL_MANGLING   :protocol-mangling
                         I2CFunctionality/QUICK               :quick
                         I2CFunctionality/READ_BLOCK          :read-block
                         I2CFunctionality/READ_BYTE           :read-byte
                         I2CFunctionality/READ_BYTE_DIRECTLY  :read-byte-directly
                         I2CFunctionality/READ_I2C_BLOCK      :read-i2c-block
                         I2CFunctionality/READ_WORD           :read-word
                         I2CFunctionality/SMBUS_PEC           :smbus-pec
                         I2CFunctionality/TEN_BIT_ADDRESSING  :bit-10-addressing
                         I2CFunctionality/TRANSACTIONS        :transactions
                         I2CFunctionality/WRITE_BLOCK         :write-block
                         I2CFunctionality/WRITE_BYTE          :write-byte
                         I2CFunctionality/WRITE_BYTE_DIRECTLY :write-byte-directly
                         I2CFunctionality/WRITE_I2C_BLOCK     :write-i2c-block
                         I2CFunctionality/WRITE_WORD          :write-word))))
          (I2CFunctionality/values))))




(defn select-slave

  "Selects an I2C slave device.

   Affects every IO operations besides transactions where the slave address is given for each message.

   Returns the given I2C bus.


   See [[]] for :bit-10-addressing.

   ```clojure
   (select-slave my-bus
                 0x42
                 {:i2c/bit-10? false
                  :i2c/force?  false})
   ```"

  ([bus slave-address]

   (select-slave bus
                 slave-address
                 nil))


  ([^I2CBus bus slave-address slave-options]

   (.selectSlave bus
                 slave-address
                 (-obtain :i2c/force?
                          slave-options)
                 (-obtain :i2c/bit-10?
                          slave-options))
   bus))




(defn set-retries

  "Sets the number of retries when communication fails.

   Does not always produce an effect depending on the underlying driver.
  
   Returns the given I2C bus."

  [^I2CBus bus retries]

  (.setRetries bus
               retries)
  bus)




(defn set-timeout

  "Sets the timeout in milliseconds for slave responses.

   Does not always produce an effect depending on the underlying driver.
  
   Returns the given I2C bus."

  [^I2CBus bus timeout-ms]

  (.setTimeout bus
               timeout-ms)
  bus)




(defn- -buffer->vec

  ;; Converts an I2C buffer to a vector.

  [^I2CBuffer buffer]

  (into []
        (for [i (range (.-length buffer))]
          (.get buffer
                i))))




(defn- -seq->buffer

  ;; Converts a seqable into an I2CBuffer.

  ^I2CBuffer

  [sq]

  (let [buffer (I2CBuffer. (count sq))]
    (doseq [[index b] (partition 2
                                 (interleave (range)
                                             sq))]
      (.set buffer
            index
            b))
    buffer))




(defn- -flags

  ;; Given flags, produces an I2CFlags object.

  ^I2CFlags

  [flags]

  (let [i2c-flags (I2CFlags.)]
    (when (contains? flags
                     :i2c/read)
      (.set i2c-flags
            I2CFlag/READ))
    (doseq [[flag
             i2c-flag] [[:i2c/bit-10?        I2CFlag/TEN_BIT_ADDRESSING]
                        [:i2c/ignore-nak?    I2CFlag/IGNORE_NAK]
                        [:i2c/no-read-ack?   I2CFlag/NO_READ_ACK]
                        [:i2c/no-start?      I2CFlag/NO_START]
                        [:i2c/revise-rw-bit? I2CFlag/REVISE_RW_BIT]]]
      (when (-obtain flag
                     flags)
        (.set i2c-flags
              i2c-flag)))
    i2c-flags))




(defn transaction

  "A transaction represents a sequence of messages, reads and writes, meant to be carried out without interruption.

  Not every device supports this feature, or sometimes only supports 1 message per transaction with defeats their purpose.
  
  Each message specifies if it is a read or a write and consists of options :

    :i2c/bit-10?         Should the 10-bit addressing mode be used ?
    :i2c/ignore-nak?     Should \"not-acknowledge\" be ignored ?
    :i2c/no-read-ack?    Should read-acks be ignored ?
    :i2c/no-start?       Should not issue any more START/address after the initial one.
    :i2c/revise-wr-bit?  Should send a read flag for writes and vice-versa (for broken slave) ?
    :i2c/slave-address   Which slave.
    :i2c/tag             Any value associated with the message, important for reads (the number of the message
                      by default).

  After the transaction is carried out, a map of tag -> bytes is returned for reads.

  See [[capability+]] for 10-bit addressing as well as other booleans flags under the :protocol-mangling capability.

  ```clojure
  (transaction some-bus
               [{:i2c/slave-address 0x42
                 :i2c/write         [24 1 2 3]}
                {:i2c/slave-address 0x42
                 :i2c/read          3
                 :i2c/tag           :my-read}])

  ;; => {:my-read [...]}
  ```"

  [^I2CBus bus messages]

  (let [length          (count messages)
        i2c-transaction (I2CTransaction. length)
        tag->buffer     (reduce (fn prepare-message [tag->buffer [i ^I2CMessage i2c-message message]]
                                  (let [[buffer
                                         tag]   (if (contains? message
                                                               :i2c/read)
                                                  [(I2CBuffer. (:i2c/read message))
                                                   (get message
                                                        :i2c/tag
                                                        i)]
                                                  [(-seq->buffer (:i2c/write message))
                                                   nil])]
                                    (.setBuffer i2c-message
                                                buffer)
                                    (.setAddress i2c-message
                                                 (-obtain :i2c/slave-address
                                                          message))
                                    (.setFlags i2c-message
                                               (-flags message))
                                    (if (nil? tag)
                                      tag->buffer
                                      (assoc tag->buffer
                                             tag
                                             buffer))))
                                {}
                                (partition 3
                                           (interleave (range)
                                                       (for [i (range length)]
                                                         (.getMessage i2c-transaction
                                                                      i))
                                                       messages)))]
    (.doTransaction bus
                    i2c-transaction)
    (reduce-kv (fn convert-buffer [tag->vec tag buffer]
                 (assoc tag->vec
                        tag
                        (-buffer->vec buffer)))
               {}
               tag->buffer)))




(defn read

  "Reads an arbitrary amount of bytes."

  [^I2CBus bus length]

  (let [buffer (I2CBuffer. length)]
    (.read bus
           buffer)
    (-buffer->vec buffer)))




(defn write

  "Writes a sequence of bytes.
  
   Returns the given I2C bus."

  [^I2CBus bus bs]

  (.write bus
          (-seq->buffer bs))
  bus)
