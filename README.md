# Icare

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/icare.svg)](https://clojars.org/dvlopt/icare)

<!> This project has been renamed and move to
[dvlopt.i2c](https://github.com/dvlopt/i2c).

Easily use I2C on linux from your clojure program.

I2C buses are available at '/dev/i2c-N' as char devices where 'N' is the bus
number. In other words, one can simply read and write them just like regular
files. The only missing piece is the ioctl system call for selecting slave
devices. It is provided by this library through JNA which means the user does
not need to install any native dependencies.

## Usage

Read the full [API](https://dvlopt.github.io/doc/icare/index.html).

```clj
(require '[icare.core :as i2c])


;; open a bus
(def bus (i2c/open "/dev/i2c-1"))


;; select slave 0x76
(i2c/select bus
            0x76)


;; write byte 0xa2 to register 0x55
(i2c/write-byte bus
                0x55
                0xa2)


;; read 8 bytes into a byte array
(def ba (byte-array 8))

(i2c/read-bytes bus
                ba)

;; a bus is seqable
;; easily read 2 bytes
(let [[b1
       b2] (take 2
                 bus)]
  ...)


;; get a nice map showing the current status of the bus
(i2c/status bus)
;; => {:path                 "/dev/i2c-1"
;;     :extended-addressing? false
;;     :slave                0x76
;;     :close?               false}


;; do not forget the close the bus
(i2c/close bus)
```

## License

Copyright © 2017 Adam Helinski

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
