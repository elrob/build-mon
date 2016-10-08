(defproject build-mon "0.1.0"
  :description "A simple build monitor to monitor Visual Studio Online builds"
  :url "localhost:3000"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-codec "1.0.1"]
                 [bidi "2.0.12"]
                 [http-kit "2.2.0"]
                 [cheshire "5.6.3"]
                 [hiccup "1.0.5"]
                 [de.ubercode.clostache/clostache "1.4.0"]]
  :main ^:skip-aot build-mon.core
  :resource-paths ["resources"]
  :target-path "target/%s"
  :profiles {:dev {:aliases {"test" ["do" "clean"
                                     ["cljfmt" "check"]
                                     ["bikeshed" "-m" "120"]
                                     ["eastwood"]
                                     ["kibit"]
                                     ["midje"]
                                     ["cloverage" "--runner" ":midje"]]}
                   :dependencies [[midje "1.8.3"]
                                  [http-kit.fake "0.2.2"]]
                   :plugins  [[lein-midje "3.2"]
                              [lein-cloverage "1.0.7"]
                              [lein-cljfmt "0.5.6"]
                              [lein-bikeshed "0.3.0"]
                              [jonase/eastwood "0.2.3"]
                              [lein-kibit "0.1.2"]
                              [lein-ancient "0.6.10"]]}
             :uberjar {:aot :all}})
