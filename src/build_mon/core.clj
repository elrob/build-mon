(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [clj-http.client :as client])
  (:gen-class))

(defn generate-html [status result]
  (str "<head><meta http-equiv=\"refresh\" content=\"20\" /></head>
       <body style=\"background-color:" (cond (= result "succeeded") "green"
                                              (nil? result) "yellow"
                                              :default "red")
       ";\">"
       (if result
         (str "<h1 style=\"color:white;font-size:400%;\">" result "</h1>")
         (str "<h1 style=\"color:black;font-size:400%;\">" status "</h1>"))
       "</body>"))

(defn handler [account project token request]
  (let [last-build-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                            project "/_apis/build/builds?api-version=2.0&$top=1")
        last-build (-> (client/get last-build-url
                                   {:accept :json :as :json
                                    :basic-auth ["USERNAME CAN BE ANY VALUE" token]})
                       :body :value first)
        status (:status last-build)
        result (:result last-build)]
      (prn "--")
      (prn "Status:" status)
      (prn "Result:" result)
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (generate-html status result)}))

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (ring-jetty/run-jetty (partial handler vso-account vso-project vso-personal-access-token)
                            {:port port})
      (prn "App didn't start due to missing parameters."))))
