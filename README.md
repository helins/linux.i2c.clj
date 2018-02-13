# dvlopt.i2c

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/i2c.svg)](https://clojars.org/dvlopt/i2c)

Easily use [I2C](https://en.wikipedia.org/wiki/I%C2%B2C) from your clojure
program.

On linux, I2C buses are available at '/dev/i2c-N' as char devices where 'N' is the bus
number. This clojure library allows the user to talk to slave device using such
a bus.

## Usage

Read the [API](https://dvlopt.github.io/doc/dvlopt/i2c/).

All functions are specified using clojure.spec.

In short, without error checking :

```clj
(require '[dvlopt.i2c :as i2c])


;; Open the needed bus.

(def bus
     (::i2c/bus (i2c/open "/dev/i2c-1")))


;; Select slave 0x76.

(i2c/select-slave bus
                  0x76)


;; Write byte 0xa2 to register 0x55.

(i2c/write-byte bus
                0x55
                0xa2)


;; Read 8 bytes into a byte array.

(def ba
     (byte-array 8))

(i2c/read-bytes bus
                ba)


;; Do not forget the close the bus when done.

(i2c/close bus)
```

## License

Copyright © 2017-2018 Adam Helinski

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
