(ns dvlopt.i2c

  "Talk to I2C devices.
  
   All functions are specified using clojure.spec."

  {:author "Adam Helinski"}

  (:require [clojure.spec.alpha :as s]
            [dvlopt.ex          :as ex]
            [dvlopt.void        :as void])
  (:import com.sun.jna.Native
           com.sun.jna.NativeLong
           dvlopt.i2c.CLib
           (java.io FileNotFoundException
                    IOException
                    RandomAccessFile)
           (sun.misc JavaIOFileDescriptorAccess
                     SharedSecrets)))




;;;;;;;;;; Declarations


(declare I2C)




;;;;;;;;;; Specs - Misc


(s/def ::errno

  (s/int-in 1
            134))


(s/def ::exception

  ::ex/Throwable)


(s/def ::int.pos

  (s/int-in 0
            (inc Integer/MAX_VALUE)))


(s/def ::string

  (s/and string?
         not-empty))




;;;;;;;;;; Specs - I2C


(s/def ::address

  (s/int-in 0
            512))


(s/def ::addressing

  #{:7-bit
    :10-bit})


(s/def ::bus

  #(instance? I2C
              %))


(s/def ::byte

  (s/int-in 0
            256))


(s/def ::closed?

  boolean?)


(s/def ::length

  ::int.pos)


(s/def ::path

  ::string)


(s/def ::register

  ::byte)




;;;;;;;;;; Specs - Function return values


(s/def ::result.io

  (s/or :success ::success.io
        :failure ::failure.io))


(s/def ::success.io

  nil?)

(s/def ::failure.io

  (s/keys :req [::blame.io]
          :opt [::exception]))


(s/def ::blame.io

  #{:io
    :unknown})




(s/def ::result.ioctl

  (s/or :success ::success.io
        :failure ::failure.ioctl))


(s/def ::failure.ioctl

  (s/keys :req [::blame.ioctl
                ::errno]))


(s/def ::blame.ioctl

  #{:native})




(s/def ::result.open

  (s/or :success ::success.open
        :failure ::failure.open))


(s/def ::success.open

  (s/keys :req [::bus]))


(s/def ::failure.open

  (s/keys :req [::blame.open
                ::exception]))


(s/def ::blame.open

  #{:not-allowed
    :not-found
    :unknown})




(s/def ::result.read-byte

  (s/or :success (s/keys :req [::byte])
        :failure ::failure.read))


(s/def ::result.read-bytes

  (s/or :success (s/keys :req [::length])
        :failure ::failure.read))


(s/def ::failure.read

  (s/keys :req [::blame.read]
          :opt [::exception]))


(s/def ::blame.read

  (s/or :io    ::blame.io
        :read #{:eof}))




(s/def ::status

  (s/keys :req [::addressing
                ::closed?
                ::path]
          :opt [::address]))




;;;;;;;;;; Private - IO - Native


(def ^:private -ioctl--I2C_SLAVE

  "Request long for selecting a slave device via ioctl."

  (NativeLong. 0x0703
               true))




(def ^:private -ioctl--I2C_TENBIT

  "Request long for choosing between 7 bits and 10 bits addressing via `ioctl`."

  (NativeLong. 0x0704
               true))


  

(def ^:private ^JavaIOFileDescriptorAccess -fd-access

  "Object for accessing the linux file descriptor of a java file."
  
  (SharedSecrets/getJavaIOFileDescriptorAccess))




(defn- -fd

  "Gets the linux file descriptor of a java file."

  [^RandomAccessFile file]

  (.get -fd-access
        (.getFD file)))




(defn- -ioctl

  "Issues an ioctl call."

  [fd request value]

  (when (= (CLib/ioctl fd
                       request
                       value)
           -1)
    {::blame.ioctl :native
     ::errno       (Native/getLastError)}))




;;;;;;;;;; Private - IO - Read and write


(defn- -read-result

  "Prepares a read result while taking into account EOF's."

  [kw n]

  (if (= n
         -1)
    {::blame.read :eof}
    {kw n}))




(defn- -read-byte

  "Reads a single byte from a file."

  [^RandomAccessFile file]

  (-read-result ::byte
                (.read file)))




(defn- -read-bytes

  "Reads several bytes from a file into the given byte array."

  ([^RandomAccessFile file ba]

   (-read-result ::length
                 (.read file
                        ba)))


  ([^RandomAccessFile file ba offset length]

   (-read-result ::length
                 (.read file
                        ba
                        offset
                        length))))




(defn- -select-register

  "Selects a I2C register."

  [^RandomAccessFile file ^long register]

  (.write file
          register)
  nil)




(defn- -write-byte

  "Write a single byte to a file."

  [^RandomAccessFile file ^long b]

  (.write file
          b)
  nil)




(defn- -write-bytes

  "Write several bytes from a byte array to a file."

  ([^RandomAccessFile file ^bytes ba]

   (.write file
           ba)
   nil)


  ([^RandomAccessFile file ba offset length]

   (.write file
           ba
           offset
           length)
   nil))




;;;;;;;;;; Private - Exception handling


(defn- -try

  "Wraps forms in a try-catch form."

  [blame forms]

  `(try
     ~@forms
     (catch IOException e#
       {~blame      :io
        ::exception e#})
     (catch Throwable e#
       {~blame      :unknown
        ::exception e#})))




(defmacro ^:private -try-io

  "Try an IO operation."

  [& forms]

  (-try ::blame.io
        forms))




(defmacro ^:private -try-read

  "Try a read operation."

  [& forms]

  (-try ::blame.read
        forms))




;;;;;;;;;; API - Protocol


(defprotocol I2C

  "Commands an I2C bus."


  (addressing [this mode]

    "Issues an ioctl call to choose an addressing scheme :

       :7-bit
       :10-bit")


  (close [this]
         
    "Closes this bus.")


  (read-byte [this]
             [this register]

    "Reads a single byte directly or from a register.")
    

  (read-bytes [this byte-array]
              [this register byte-array]
              [this byte-array offset length]
              [this register byte-array offset length]

    "Reads several bytes into a byte array, directly or from a register.")


  (select-slave [this address]

    "Issues an ioctl call to select a slave device.")


  (status [this]

    "Returns a map describing the current state of this bus.")


  (write-byte [this byte]
              [this register byte]

    "Writes a single byte directly or to a register.")


  (write-bytes [this byte-array]
               [this register byte-array]
               [this byte-array offset length]
               [this register byte-array offset length]

    "Writes a byte array directly or to a register."))




;;;;;;;;;; API - Protocol - Specs


(s/fdef addressing

  :args (s/cat :bus  ::bus
               :mode ::addressing)
  :ret  ::result.ioctl)


(s/fdef close

  :args (s/cat :bus ::bus)
  :ret  ::result.io)


(s/fdef select-slave

  :args (s/cat :bus     ::bus
               :address ::address)
  :ret  ::result.ioctl)


(s/fdef read-byte

  :args (s/cat :bus      ::bus
               :register (s/? ::register))
  :ret  ::result.read-byte)


(s/fdef read-bytes

  :args (s/cat :bus        ::bus
               :register   (s/? ::int.pos)
               :byte-array bytes?
               :opts       (s/? (s/cat :offset ::int.pos
                                       :length ::length)))
  :ret  ::result.read-bytes)


(s/fdef status

  :args (s/cat :bus ::bus)
  :ret  ::status)


(s/fdef write-byte

  :args (s/cat :bus      ::bus
               :register (s/? ::register)
               :byte     ::byte)
  :ret  ::result.io)


(s/fdef write-bytes

  :args (s/cat :bus        ::bus
               :register   (s/? ::register)
               :byte-array bytes?
               :opts       (s/? (s/cat :offset ::int.pos
                                       :length ::length)))
  :ret  ::result.io)




;;;;;;;;;; API - Protocol - Implementation


(deftype I2CBus [-path
                 ^RandomAccessFile  -file
                 ^long              -fd
                 ^:volatile-mutable -addressing
                 ^:volatile-mutable -slave
                 ^:volatile-mutable -closed?]


  I2C


    (addressing [_ mode]
      (when (not= mode
                  -addressing)
        (or (-ioctl -fd
                    -ioctl--I2C_TENBIT
                    (condp identical?
                           mode
                      :7-bit  0
                      :10-bit 1))
            (do 
              (set! -addressing
                    mode)
              nil))))


    (close [_]
      (when-not -closed?
        (-try-io
          (.close -file)
          (set! -closed?
                true)
          nil)))


    (read-byte [_]
      (-try-read
        (-read-byte -file)))


    (read-byte [_ register]
      (-try-read
        (-select-register -file
                          register)
        (-read-byte -file)))


    (read-bytes [_ ba]
      (-try-read
        (-read-bytes -file
                     ba)))


    (read-bytes [_ register ba]
      (-try-io
        (-select-register -file
                          register)
        (-read-bytes -file
                     ba)))


    (read-bytes [_ ba offset length]
      (-try-read
        (-read-bytes -file
                     ba
                     offset
                     length)))


    (read-bytes [_ register ba offset length]
      (-try-read
        (-select-register -file
                          register)
        (-read-bytes -file
                     ba
                     offset
                     length)))


    (select-slave [_ address]
      (when (not= address
                  -slave)
        (or (-ioctl -fd
                    -ioctl--I2C_SLAVE
                    address)
            (do
              (set! -slave
                    address)
              nil))))


    (status [_]
      (void/assoc-some {::path       -path
                        ::addressing -addressing
                        ::closed?    -closed?}
                       ::slave -slave))


    (write-byte [_ b]
      (-try-io
        (-write-byte -file
                     b)))


    (write-byte [_ register b]
      (-try-io
        (-select-register -file
                          register)
        (-write-byte -file
                     b)))


    (write-bytes [_ ba]
      (-try-io
        (-write-bytes -file
                      ba)))


    (write-bytes [_ register ba]
      (-try-io
        (-select-register -file
                          register)
        (-write-bytes -file
                      ba)))


    (write-bytes [_ ba offset length]
      (-try-io
        (-write-bytes -file
                      ba
                      offset
                      length)))


    (write-bytes [_ register ba offset length]
      (-try-io
        (-select-register -file
                          register)
        (-write-bytes -file
                      ba
                      offset
                      length))))




;;;;;;;;;; API - Acquire a bus


(s/fdef open

  :args (s/cat :path ::path)
  :ret  ::result.open)


(defn open

  "Opens an I2C bus at the given path.

   The user running the program needs R/W permissions for this file."

  [^String path]

  (try
    (let [file (RandomAccessFile. path
                                  "rw")]
      {::bus (I2CBus. path
                      file
                      (-fd file)
                      :7-bit
                      nil
                      false)})
    (catch FileNotFoundException e
      {::blame.open :not-found
       ::exception  e})
    (catch SecurityException e
      {::blame.open :not-allowed
       ::exception  e})
    (catch Throwable e
      {::blame.open :unknown
       ::exception  e})))
