# dvlopt.linux.i2c

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/linux.i2c.svg)](https://clojars.org/dvlopt/linux.i2c)

Easily use [I2C](https://en.wikipedia.org/wiki/I%C2%B2C) from your clojure
program.

Based on [linux-i2c.java](https://github.com/dvlopt/linux-i2c.java). This
library provides an API around the standard Linux interface for talking to slave
devices.

For information about running Clojure on the Raspberry Pi, here is a
[guide](https://github.com/dvlopt/clojure-raspberry-pi).

## Usage

Read the
[API](https://dvlopt.github.io/doc/clojure/dvlopt/linux.i2c/index.html).

In short, without error checking :

```clj
(require '[dvlopt.linux.i2c       :as i2c]
         '[dvlopt.linux.i2c.smbus :as smbus])


(with-open [^java.lang.AutoCloseable bus (i2c/bus "dev/i2c-1")]

    ;; Selects a slave device.
    (i2c/select-slave bus
                      0x24)

    ;; Reads 8 bytes.
    (i2c/read bus
              8)
    => [...]

    ;; Write a few bytes
    (i2c/write bus
               [42 1 2 3])

    ;; Does a transactions, several messages without interruption.

    (i2c/transaction bus
                     [{::i2c/slave-address 0x24
                       ::i2c/write         [42 1 2 3]}
                      {::i2c/slave-address 0x24
                       ::i2c/read          4
                       ::i2c/tag           :some-read}])

    => {:some-read [...]}

    ;; A few SMBus operations.

    (smbus/quick-write bus)

    (smbus/read-byte bus
                     42)

    (smbus/write-block bus
                       43
                       [1 2 3])
    )
```

## License

Copyright © 2017-2018 Adam Helinski

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
