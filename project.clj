(defproject phreedom/ipp "0.1.0-SNAPSHOT"
  :description "Library implementing the server-side of IPP"
  :url "https://github.com/phreedom/clojure-ipp"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.van-clj/zetta-parser "0.1.0"]; parser

                 [byte-streams "0.2.2"];for debug printing of ipp requests

                 ;; Web
                 [ring/ring-core "1.4.0"]
                 [org.immutant/web "2.1.0"]
                 [org.immutant/scheduling "2.1.0"]]
  :repl-options {:init-ns ipp.test-instance}
)
