(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(defn succeeded? [build] (= (:result build) "succeeded"))

(defn in-progress? [build] (nil? (:result build)))

(defn determine-background-colour [build previous-build]
  (cond (succeeded? build) "green"
        (and (in-progress? build) (succeeded? previous-build)) "yellow"
        (and (in-progress? build) (not (succeeded? previous-build))) "orange"
        :default "red"))

(defn generate-html [build previous-build]
  (let [background-colour (determine-background-colour build previous-build)
        font-colour (if (in-progress? build) "black" "white")
        text (if (in-progress? build) (:status build) (:result build))]
    (str "<head>"
         "<title>Build Status</title>"
         "<meta http-equiv=\"refresh\" content=\"20\" />"
         "<link rel=\"shortcut icon\" href=\"/favicon_" background-colour ".ico\" />"
         "</head>"
         "<body style=\"background-color:" background-colour ";\">"
         "<h1 style=\"color:" font-colour ";font-size:400%;\">" text "</h1>"
         "</body>")))

(defn handler [account project token request]
  (let [last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2")
        api-response (client/get last-two-builds-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
        [build previous-build] (try (-> api-response :body (json/parse-string true) :value)
                                    (catch Exception e))]
    (prn "Build - Result: " (:result build))
    (prn "Build - Status: " (:status build))
    (prn "Prev  - Result: " (:result build))
    (prn "Prev  - Status: " (:status build))
    (when build
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (generate-html build previous-build)})))

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [wrapped-handler (-> (partial handler vso-account vso-project vso-personal-access-token)
                                (resource/wrap-resource "public"))]
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (prn "App didn't start due to missing parameters."))))
