(ns user

  "For daydreaming at the REPL."

  (:require [helins.linux.i2c                 :as i2c]
            [helins.linux.i2c.example.arduino :as arduino]
            [helins.linux.i2c.smbus           :as i2c.smbus])
  (:import (com.sun.jna Memory
                        Native
                        Pointer)
           (io.helins.linux.i2c I2CBuffer
                                I2CBus
                                I2CFlag
                                I2CFlags
                                I2CFunctionalities
                                I2CFunctionality
                                I2CMessage
                                I2CTransaction
                                SMBus
                                SMBus$Block)
           java.nio.ByteBuffer))



;;;;;;;;;;


(comment
  

  (def bus
       (i2c/bus "/dev/i2c-1"))


  )
