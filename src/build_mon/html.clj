(ns build-mon.html
  (:require [hiccup.core :as hiccup]
            [clojure.string :as s]))

(def refresh-icon [:div.refresh-icon.hidden [:i.fa.fa-refresh.fa-spin.fa-3x]])

(def error-modal [:div.error-modal.hidden
                  [:div.error-modal-background]
                  [:h1.error-modal-text "Build Monitor Error"]])

(defn- refresh-html [refresh-info]
  (list [:link {:rel "stylesheet" :href
                "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
        [:script
         (str "window.buildDefinitionIds = [" (s/join "," (:build-definition-ids refresh-info)) "];")
         (str "window.refreshSeconds = " (:refresh-interval refresh-info) ";")]
        [:script {:src "/refresh.js" :defer "defer"}]))

(defn- now [] (java.util.Date.))

(defn- generate-build-panel [{:keys [build-definition-name build-definition-id build-number
                                    status-text state commit-message]}]
  [:a {:href (str "/build-definitions/" build-definition-id)}
   [:div {:id (str "build-definition-id-" build-definition-id) :class (str "build-panel " (name state))}
    [:h1.status.hide-overflow status-text]
    [:h1.build-definition-name.hide-overflow build-definition-name]
    [:h1.build-number.hide-overflow build-number]
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

