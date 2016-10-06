(ns build-mon.html
  (:require [hiccup.core :as hiccup]
            [clojure.string :as s]))

(def refresh-icon [:div.refresh-icon.hidden [:i.fa.fa-refresh.fa-spin.fa-3x]])

(def error-modal [:div.error-modal.hidden
                  [:div.error-modal-background]
                  [:h1.error-modal-text "Build Monitor Error"]])

;RELEASE
(defn- generate-release-env-panels [{:keys [release-definition-name
                                            release-definition-id
                                            release-number
                                            release-environments]}]
  (map (fn [release-env]
         (let [env-name (:env-name release-env)
               env-state (:state release-env)]
           [:div {:id (str "release-definition-id-" release-definition-id "-" env-name)
                  :class (str "panel " (name env-state))}
            [:span.tag.release "RELEASE"]
            [:h1.release-definition-name release-definition-name]
            [:hr]
            [:h1.release-number release-number]
            [:hr]
            [:h1.release-env-name env-name]]))
       release-environments))

;BUILD
(defn- generate-build-panel [{:keys [build-definition-name build-definition-id build-number
                                     status-text state commit-message]}]
  [:div {:id (str "build-definition-id-" build-definition-id) :class (str "panel " (name state))}
   [:span.tag.build "BUILD"]
   [:h1.build-definition-name build-definition-name]
   [:hr]
   [:h1.build-number build-number]
   [:hr]
   [:br]
   [:div.commit-message commit-message]])

; ==================================================
; ATTEMPT AT UNIVERSAL HTML GENERATOR
; ==================================================

(defn- count-total-releases [release-info-maps]
  (reduce + (map (fn [rel-info-map] (count (:release-environments rel-info-map))) release-info-maps)))

(defn generate-build-monitor-html [build-info-maps release-info-maps favicon-path]
  (let [total-builds (count build-info-maps)
        total-releases (count-total-releases release-info-maps)
        max-panels-per-row 4
        panel-rows (Math/ceil (/ (+ total-builds total-releases) max-panels-per-row))
        panel-width 25
        panel-height (/ 100 panel-rows)]
    (hiccup/html
     [:head
      [:title "Build Monitor"]
      [:link#favicon {:rel "shortcut icon" :href favicon-path}]
      [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans|Roboto"}]
      [:link {:rel "stylesheet" :href
              "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
      [:link {:rel "stylesheet " :href "/style.css" :type "text/css"}]]
     [:style (str ".panel { width:" panel-width "%; height:" panel-height "%; }")]
     [:body
      refresh-icon
      error-modal
      (map generate-build-panel build-info-maps)
      (map generate-release-env-panels release-info-maps)
      [:script {:src "/refresh.js" :defer "defer"}]])))
