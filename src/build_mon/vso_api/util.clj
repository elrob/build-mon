(ns build-mon.vso-api.util
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn- get-json-body [response]
  (-> response :body (json/parse-string true)))

(defn vso-api-get-fn [token]
  (fn [url] (let [response @(http/get url {:basic-auth [nil token]
                                           :accept :json :follow-redirects false})]
              (if (= (:status response) 200)
                (get-json-body response)
                (throw (ex-info "Response status was not 200"
                                {:url url :response response}))))))
