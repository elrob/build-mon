(ns build-mon.vso-api.util
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn- validate-200-response [response]
  (if (= (:status response) 200)
    response
    (throw (ex-info "Response status was not 200" {:response response}))))

(defn get-json-body [get-fn url]
  (-> url get-fn validate-200-response :body (json/parse-string true)))

(defn vso-api-get-fn [token]
  (fn [url] @(http/get url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]
                            :accept :json :follow-redirects false})))
