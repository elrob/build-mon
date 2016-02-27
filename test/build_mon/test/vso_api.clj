(ns build-mon.test.vso-api
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.vso-api :as api]))

(fact "vso-api-get-fn wraps an api token and returns a function which can be called with just a URL"
      (let [return-value (api/vso-api-get-fn "SOME API TOKEN")]
        (fn? return-value) => truthy
        (return-value "NOT A REAL URL") => (throws java.net.MalformedURLException)))

