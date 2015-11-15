(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(defn generate-html [status result]
  (let [background-colour (cond (= result "succeeded") "green"
                                (nil? result) "yellow"
                                :default "red")
        font-colour (if result "white" "black")
        text (if result result status)]
    (str "<head>"
         "<title>Build Status</title>"
         "<meta http-equiv=\"refresh\" content=\"20\" />"
         "<link rel=\"shortcut icon\" href=\"/favicon_" background-colour ".ico\" />"
         "</head>"
         "<body style=\"background-color:" background-colour ";\">"
         "<h1 style=\"color:" font-colour ";font-size:400%;\">" text "</h1>"
         "</body>")))

(defn handler [account project token request]
  (let [last-build-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                            project "/_apis/build/builds?api-version=2.0&$top=1")
        api-response (client/get last-build-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
        last-build (try (-> api-response :body (json/parse-string true) :value first)
                        (catch Exception e))]
    (when last-build
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (generate-html (:status last-build) (:result last-build))})))

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [wrapped-handler (-> (partial handler vso-account vso-project vso-personal-access-token)
                                (resource/wrap-resource "public"))]
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (prn "App didn't start due to missing parameters."))))
