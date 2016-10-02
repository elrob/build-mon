(ns build-mon.test.vso-api.util
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.vso-api.util :as util]))

(fact "vso-api-get-fn wraps an api token and returns a function which makes a HTTP GET request on a url"
      (let [result (util/vso-api-get-fn "VSO_API_TOKEN")]
        (fn? result) => truthy
        (result "NOT A REAL URL") => (contains {:error anything})))
