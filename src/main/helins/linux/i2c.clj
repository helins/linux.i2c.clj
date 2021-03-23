;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.linux.i2c

  "The Linux kernel provides a standard interface for performing I2C operations.
  
   This library exposes this interface in a clojure idiomatic way.

   Each IO operation might throw if something fails.

   Essentially, IO can be performed by directly reading and writing arbitrary bytes, doing
   transactions (uninterrupted sequence of messages) and using standard SMBus operations.

   Not everything is supported by your driver, refer to `capabilities`. Furthermore, slave devices
   are often buggy and imperfect."

  {:author "Adam Helinski"}

  (:import (io.helins.linux.i2c I2CBuffer
                                I2CBus
                                I2CMessage
                                I2CFlag
                                I2CFlags
                                I2CFunctionalities
                                I2CFunctionality
                                I2CTransaction)
           java.lang.AutoCloseable)
  (:refer-clojure :exclude [read]))


;;;;;;;;;; Default values


(def defaults

  "Defaults values for options used throughout this library."

  {::10-bit?        false
   ::force?         false
   ::ignore-nak?    false
   ::no-read-ack?   false
   ::no-start?      false
   ::revise-rw-bit? false
   ::slave-address  0})



(defn- -obtain

  ;;

  [k hmap]

  (or (get hmap
           k)
      (get defaults k)))


;;;;;;;;;;


(defn bus

  "Opens an I2C bus by providing the number of the bus or a direct path.
  
   
   Ex. (bus \"/dev/i2c-1\")"

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




(defn capabilities

  "Retrieves the capabilities of the given bus.

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
                         I2CFunctionality/TEN_BIT_ADDRESSING  :10-bit-addressing
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


   Cf. `capabilities` for :10-bit-addressing.


   Ex. (select-slave some-bus
                     0x42
                     {::10-bit? false
                      ::force?  false})"

  ([bus slave-address]

   (select-slave bus
                 slave-address
                 nil))


  ([^I2CBus bus slave-address slave-options]

   (.selectSlave bus
                 slave-address
                 (-obtain ::force?
                          slave-options)
                 (-obtain ::10-bit?
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
                     ::read)
      (.set i2c-flags
            I2CFlag/READ))
    (doseq [[flag
             i2c-flag] [[::10-bit?        I2CFlag/TEN_BIT_ADDRESSING]
                        [::ignore-nak?    I2CFlag/IGNORE_NAK]
                        [::no-read-ack?   I2CFlag/NO_READ_ACK]
                        [::no-start?      I2CFlag/NO_START]
                        [::revise-rw-bit? I2CFlag/REVISE_RW_BIT]]]
      (when (-obtain flag
                     flags)
        (.set i2c-flags
              i2c-flag)))
    i2c-flags))




(defn transaction

  "A transaction represents a sequence of messages, reads and writes, meant to be carried out without interruption.

  Not every device supports this feature, or sometimes only supports 1 message per transaction with defeats their purpose.
  
  Each message specifies if it is a read or a write and consists of options :

    ::10-bit?         Should the 10-bit addressing mode be used ?
    ::ignore-nak?     Should \"not-acknowledge\" be ignored ?
    ::no-read-ack?    Should read-acks be ignored ?
    ::no-start?       Should not issue any more START/address after the initial one.
    ::revise-wr-bit?  Should send a read flag for writes and vice-versa (for broken slave) ?
    ::slave-address   Which slave.
    ::tag             Any value associated with the message, important for reads (the number of the message
                      by default).

  After the transaction is carried out, a map of tag -> bytes is returned for reads.

  Cf. `capabilities` for :10-bit-addressing as well as other booleans flags under the :protocol-mangling capability.
  
  
  Ex. (transaction some-bus
                   [{::slave-address 0x42
                     ::write         [24 1 2 3]}
                    {::slave-address 0x42
                     ::read          3
                     ::tag           :my-read}])

      => {:my-read [...]}"

  [^I2CBus bus messages]

  (let [length          (count messages)
        i2c-transaction (I2CTransaction. length)
        tag->buffer     (reduce (fn prepare-message [tag->buffer [i ^I2CMessage i2c-message message]]
                                  (let [[buffer
                                         tag]   (if (contains? message
                                                               ::read)
                                                  [(I2CBuffer. (::read message))
                                                   (get message
                                                        ::tag
                                                        i)]
                                                  [(-seq->buffer (::write message))
                                                   nil])]
                                    (.setBuffer i2c-message
                                                buffer)
                                    (.setAddress i2c-message
                                                 (-obtain ::slave-address
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
