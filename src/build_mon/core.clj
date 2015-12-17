(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(def background-colours {:green "green" :yellow "yellow" :orange "orange" :red "red"})

(defn succeeded? [build] (= (:result build) "succeeded"))

(defn in-progress? [build] (nil? (:result build)))

(defn background-colour-key->favicon-filename [background-colour]
  (str "favicon_" (name background-colour) ".ico"))

(defn determine-background-colour [build previous-build]
  (cond (succeeded? build) :green
        (and (in-progress? build) (succeeded? previous-build)) :yellow
        (and (in-progress? build) (not (succeeded? previous-build))) :orange
        :default :red))

(defn determine-status-text [build]
  (if (in-progress? build) (:status build) (:result build)))

(defn tryparse [string]
  (try (Integer. string)
       (catch Exception e nil)))

(defn determine-refresh-interval [params]
  (let [refresh (get params "refresh")]
    (case refresh
      (nil "true" "yes") 20
      ("false" "no") nil
      (tryparse refresh))))

(defn generate-html [build previous-build commit-message refresh]
  (let [background-colour (determine-background-colour build previous-build)
        favicon-filename (background-colour-key->favicon-filename background-colour)
        font-colour (if (in-progress? build) "black" "white")
        status-text (determine-status-text build)]
    (str "<head>"
         "<title>Build Status</title>"
         (when refresh (str "<meta http-equiv=\"refresh\" content=\"" refresh "\" />"))
         "<link rel=\"shortcut icon\" href=\"" favicon-filename "\" />"
         "</head>"
         "<body style=\"background-color:" (background-colour background-colours) ";\">"
         "<h1 style=\"color:" font-colour ";font-size:400%;text-align:center;\">" status-text "</h1>"
         "<h1 style=\"color:" font-colour ";font-size:300%;text-align:center;\">" (:buildNumber build) "</h1>"
         "<div style=\"color:" font-colour ";font-size:300%;text-align:center;\">" commit-message "</div>"
         "</body>")))

(defn get-commit-message [account token build]
  (let [repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (client/get commit-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :comment)
         (catch Exception e))))

(defn get-last-two-builds [account project token]
  (let [last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2")]
    (try (-> (client/get last-two-builds-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))

(defn handler [account project token request]
  (let [[build previous-build] (get-last-two-builds account project token)
        commit-message (get-commit-message account token build)
        refresh (determine-refresh-interval (:query-params request))]
    (prn "--------------------------------------")
    (prn (str "Commit message: " commit-message))
    (prn (str "Build - Result: " (:result build)))
    (prn (str "Build - Status: " (:status build)))
    (prn (str "Prev  - Result: " (:result build)))
    (prn (str "Prev  - Status: " (:status build)))
    (prn "--------------------------------------")
    (when build
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (generate-html build previous-build commit-message refresh)})))

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [wrapped-handler (-> (partial handler vso-account vso-project vso-personal-access-token)
                                (resource/wrap-resource "public")
                                (params/wrap-params))]
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (prn "App didn't start due to missing parameters."))))
