(ns build-mon.test.vso-api
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.vso-api :as api]))

(fact "vso-api-get-fn wraps an api token and returns a function which can be called with just a URL"
      (let [result (api/vso-api-get-fn "SOME API TOKEN")]
        (fn? result) => truthy
        (result "NOT A REAL URL") => (throws java.net.MalformedURLException)))

(fact "vso-api-fns returns a map of the exposed functions"
      (let [get-fn (fn [url] "API RESPONSE")
            result (api/vso-api-fns get-fn "ACCOUNT_NAME" "PROJECT_NAME")]
        (fn? (:retrieve-build-info result)) => truthy
        (fn? (:retrieve-build-definitions result)) => truthy))
