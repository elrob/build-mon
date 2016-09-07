(ns build-mon.html
  (:require [hiccup.core :as hiccup]
            [clojure.string :as s]))


(defn- now [] (java.util.Date.))

(def refresh-icon [:div.refresh-icon.hidden [:i.fa.fa-refresh.fa-spin.fa-3x]])

(def error-modal [:div.error-modal.hidden
                  [:div.error-modal-background]
                  [:h1.error-modal-text "Project Monitor Error"]])

; TODO: update with release info
(defn- refresh-html [refresh-info]
  (list [:link {:rel "stylesheet" :href
                "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
        [:script
         (str "window.buildDefinitionIds = [" (s/join "," (:build-definition-ids refresh-info)) "];")
         (str "window.releaseDefinitionIds = [" (s/join "," (:release-definition-ids refresh-info)) "];")
         (str "window.refreshSeconds = " (:refresh-interval refresh-info) ";")]
        [:script {:src "/refresh.js" :defer "defer"}]))



;RELEASE
(defn- generate-release-panel [{:keys [
                                        release-definition-name
                                        release-definition-id
                                        release-number
                                        state
                                        ;commit-message
                                        ]}]
  [:a {:href (str "/release-definitions/" release-definition-id)}
   [:div {:id (str "release-definition-id-" release-definition-id) :class (str "panel " (name state))}
    [:h1.release-definition-name release-definition-name]
    [:hr]
    [:h1.release-number release-number]
    [:hr]
    ; [:div.commit-message commit-message]
    ]])

;BUILD
(defn- generate-build-panel [{:keys [build-definition-name build-definition-id build-number
                                    status-text state commit-message]}]
  [:a {:href (str "/build-definitions/" build-definition-id)}
   [:div {:id (str "build-definition-id-" build-definition-id) :class (str "panel " (name state))}
    [:h1.build-definition-name build-definition-name]
    [:hr]
    [:h1.build-number build-number]
    [:hr]
    [:br]
    [:div.commit-message commit-message]]])

(defn generate-build-monitor-html [build-info-maps refresh-info favicon-path]
  (let [total-builds (count build-info-maps)
        max-panels-per-row 4
        panel-rows (Math/ceil (/ total-builds max-panels-per-row))
        padding 2
        panel-width (- 25 (* padding 2))
        panel-height (- (/ 100 panel-rows) (* padding 2))
        ]
  (hiccup/html
    [:head
     [:title "Build Monitor"]
     [:link#favicon {:rel "shortcut icon" :href favicon-path}]
     (when refresh-info (refresh-html refresh-info))
     [:link {:rel "stylesheet ":href "/style.css" :type "text/css"}]]
     [:style (str ".build-panel { width:" panel-width "%; height:" panel-height "%;}")]
    [:body
     refresh-icon
     error-modal
     (map generate-build-panel build-info-maps)])))


; ==================================================
; ATTEMPT AT UNIVERSAL HTML GENERATOR
; ==================================================

(defn generate-universal-monitor-html [build-info-maps release-info-maps refresh-info favicon-path]
  (let [total-builds (count build-info-maps)
        total-releases (count release-info-maps)
        max-panels-per-row 4
        panel-rows (Math/ceil (/ (+ total-builds total-releases) max-panels-per-row))
        padding 2
        panel-width (- 25 (* padding 2))
        panel-height (- (/ 100 panel-rows) (* padding 2))
        ]
  (hiccup/html
    [:head
     [:title "Project Monitor"]
     [:link#favicon {:rel "shortcut icon" :href favicon-path}]
     [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans|Roboto"}]

     (when refresh-info (refresh-html refresh-info))
     [:link {:rel "stylesheet ":href "/style.css" :type "text/css"}]]
     [:style (str ".panel { width:" panel-width "%; height:" panel-height "%;}")]
    [:body
     refresh-icon
     error-modal
     (map generate-build-panel build-info-maps)
     (map generate-release-panel release-info-maps)])))
