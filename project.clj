(defproject dvlopt/linux.i2c
            "1.1.1"

  :description  "Use the standard Linux I2C api"
  :url          "https://github.com/dvlopt/linux.ic2.clj"
  :license      {:name "Eclipse Public License"
                 :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[dvlopt/void         "0.0.0"]
                 [io.dvlopt/linux-i2c "1.0.0"]]
  :profiles     {:dev {:source-paths ["dev"
                                      "examples"]
                       :main         user
                       :dependencies [[criterium              "0.4.4"]
                                      [org.clojure/clojure    "1.9.0"]
                                      [org.clojure/test.check "0.10.0-alpha2"]]
                       :plugins      [[lein-codox      "0.10.3"]
                                      [venantius/ultra "0.5.2"]]
                       :codox        {:output-path  "doc/auto"
                                      :source-paths ["src"]}
                       :global-vars  {*warn-on-reflection* true}
                       :repl-options {:timeout 180000}}})
