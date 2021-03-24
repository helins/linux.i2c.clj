# Linux.I2C

[![Clojars
Project](https://img.shields.io/clojars/v/io.helins/linux.i2c.svg)](https://clojars.org/io.helins/linux.i2c)

[![Cljdoc](https://cljdoc.org/badge/io.helins/linux.i2c)](https://cljdoc.org/d/io.helins/linux.i2c)

Easily use [I2C](https://en.wikipedia.org/wiki/I%C2%B2C) and its subset
[SMBus](https://en.wikipedia.org/wiki/System_Management_Bus) from Clojure JVM.

Based on [linux-i2c.java](https://github.com/helins/linux-i2c.java) which 
provides an API around the standard Linux interface for talking to slave
devices.

## Usage

This is a small overview, the [full API is available on
Cljdoc](https://cljdoc.org/d/io.helins/linux.i2c).

In short, without error checking :

```clj
(require '[helins.linux.i2c       :as i2c]
         '[helins.linux.i2c.smbus :as smbus])

;; Selects the relevant "/dev/i2c-X" bus from the filesystem.
;;
(with-open [bus (i2c/bus "/dev/i2c-1")]

    ;; Selects a slave device.
    ;;
    (i2c/select-slave bus
                      0x24)

    ;; Reads 8 bytes.
    ;;
    (i2c/read bus
              8)
    ;; => [...]

    ;; Writes a few bytes
    ;;
    (i2c/write bus
               [42 1 2 3])

    ;; Does a transactions, several messages without interruption.
    ;;
    (i2c/transaction bus
                     [{::i2c/slave-address 0x24
                       ::i2c/write         [42 1 2 3]}
                      {::i2c/slave-address 0x24
                       ::i2c/read          4
                       ::i2c/tag           :some-read}])

    ;; => {:some-read [...]}



    ;; A few SMBus operations.

    (smbus/quick-write bus)

    (smbus/read-byte bus
                     42)

    (smbus/write-block bus
                       43
                       [1 2 3])
    )
```

### Libraries targeting specific devices

Here are examples of libraries leveraging theses utilities for targetting
specific I2C slave devices:

- [BME280](https://github.com/helins/linux.i2c.bme280.clj), a triple
    temperature-humidity-pressure sensor
- [MCP342x](https://github.com/helins/linux.i2c.mcp342x.clj), a family of A/D
    converters
- [Horter-I2HAE](https://github.com/helins/linux.i2c.horter-i2hae.clj), a simple
    A/D converter

## License

Copyright © 2017 Adam Helinski

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
