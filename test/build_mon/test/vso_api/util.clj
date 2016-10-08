(ns build-mon.test.vso-api.util
  (:require [midje.sweet :refer :all]
            [org.httpkit.fake :as http]
            [build-mon.vso-api.util :as util]))

(def some-url "https://some-url.com")

(facts "about vso-api-get-fn"
       (let [get-fn (util/vso-api-get-fn "VSO_API_TOKEN")]
         (fact "wraps an api token in a function which makes a HTTP GET request and parses json response"
               (http/with-fake-http [{:url some-url :basic-auth [nil "VSO_API_TOKEN"]}
                                     {:status 200 :body "{\"some\":\"json\"}"}]
                 (get-fn some-url) => {:some "json"}))
         (fact "throws exception when response is not 200"
               (try
                 (http/with-fake-http [some-url 503]
                   (get-fn some-url))
                 (catch Exception e
                   (.getMessage e) => "Response status was not 200"
                   (:url (ex-data e)) => some-url
                   (-> (ex-data e) :response :status) => 503)))
         (fact "throws exception when response body is not valid json"
               (http/with-fake-http [some-url {:status 200 :body "~~INVALID_JSON~~"}]
                 (get-fn some-url) => (throws Exception)))))
