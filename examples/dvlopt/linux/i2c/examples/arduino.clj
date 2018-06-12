(ns dvlopt.linux.i2c.examples.arduino

  "Meant to be used with an arduino running :
  
    https://github.com/dvlopt/linux-i2c.java/tree/master/arduino
  
   The slave device always uses address 0x42."

  {:author "Adam Helinski"}

  (:require [dvlopt.linux.i2c :as i2c]))




;;;;;;;;;;


(defn reset

  "Resets the slave device."

  [bus]

  (i2c/write bus
             [0]))




(defn direct-read

  "Directly reads 1 byte from the slave, which is always 42."

  [bus]

  (first (i2c/read bus
                   1)))




(defn simple-read

  "Reads 1 byte previously registered using `simple-write`."

  [bus]

  (i2c/write bus
             [1])
  (first (i2c/read bus
                   1)))




(defn simple-write

  "Writes and saves 1 byte which can then be retrieved using `simple-read`."

  [bus b]

  (i2c/write bus
             [2 b]))




(defn multi-read

  "Reads several bytes previously sent using `multi-write`."

  [bus]

  (i2c/write bus
             [3])
  (Thread/sleep 50)
  (i2c/read bus
            8))




(defn multi-write

  "Writes and saves up to 8 bytes which can then be retrieved using `multi-write`."

  [bus bs]

  (i2c/write bus
             (cons 4
                   (take 8
                         bs))))
