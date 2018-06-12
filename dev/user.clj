(ns user

  "For daydreaming in the repl."

  (:require [clojure.spec.alpha                :as s]
            [clojure.spec.gen.alpha            :as gen]
            [clojure.spec.test.alpha           :as st]
            [clojure.test.check.clojure-test   :as tt]
            [clojure.test.check.generators     :as tgen]
            [clojure.test.check.properties     :as tprop]
            [clojure.test                      :as t]
            [criterium.core                    :as ct]
            [dvlopt.linux.i2c                  :as i2c]
            [dvlopt.linux.i2c.examples.arduino :as examples.arduino]
            [dvlopt.linux.i2c.smbus            :as smbus]
            [dvlopt.void                       :as void])
  (:import (com.sun.jna Memory
                        Native
                        Pointer)
           (io.dvlopt.linux.i2c I2CBuffer
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
  

  )
