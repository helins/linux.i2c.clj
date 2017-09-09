(defproject dvlopt/icare
            "0.0.0-alpha0"

  :description       "Clojure lib for using I2C on linux"
  :url               "https://github.com/dvlopt/icare"
  :license           {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :codox             {:output-path  "doc/auto"
                      :source-paths ["src"]}
  :source-paths      ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies      [[net.java.dev.jna/jna "4.4.0"]]
  :profiles          {:dev {:source-paths ["dev"]
                            :main         user
                            :plugins      [[venantius/ultra "0.5.1"]
                                           [lein-midje      "3.0.0"]
                                           [lein-codox      "0.10.3"]
                                           [lein-virgil     "0.1.6"]]
                            :dependencies [[org.clojure/clojure    "1.9.0-alpha17"]
                                           [org.clojure/spec.alpha "0.1.123"]
                                           [org.clojure/test.check "0.9.0"]
                                           [criterium              "0.4.4"]]
                            :global-vars  {*warn-on-reflection* true}}})
