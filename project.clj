(defproject dvlopt/i2c
            "0.0.0"

  :description       "Talk to I2C devices from Linux"
  :url               "https://github.com/dvlopt/icare"
  :license           {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths      ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies      [[dvlopt/ex            "1.0.0"]
                      [dvlopt/void          "0.0.0"]
                      [net.java.dev.jna/jna "4.4.0"]]
  :profiles          {:dev {:source-paths ["dev"]
                            :main         user
                            :dependencies [[org.clojure/clojure    "1.9.0"]
                                           [org.clojure/test.check "0.10.0-alpha2"]
                                           [criterium              "0.4.4"]]
                            :plugins      [[venantius/ultra "0.5.2"]
                                           [lein-virgil     "0.1.6"]
                                           [lein-codox      "0.10.3"]]
                            :codox        {:output-path  "doc/auto"
                                           :source-paths ["src"]}
                            :repl-options {:timeout 180000}
                            :global-vars  {*warn-on-reflection* true}}})
