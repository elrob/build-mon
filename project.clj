(defproject build-mon "0.1.0-SNAPSHOT"
  :description "A simple build monitor to monitor Visual Studio Online builds"
  :url "localhost:3000"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]]
  :main ^:skip-aot build-mon.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
