(ns build-mon.html
  (:require [hiccup.core :as hiccup]
            [clojure.string :as s]))

(def refresh-icon [:div.refresh-icon.hidden [:i.fa.fa-refresh.fa-spin.fa-3x]])

(defn- generate-release-env-panels [{:keys [release-definition-name
                                            release-definition-id
                                            release-number
                                            release-environments]}]
  (map (fn [release-env]
         (let [env-name (:env-name release-env)
               env-state (:state release-env)]
           [:div {:id (str "release-definition-id-" release-definition-id "-" env-name)
                  :class (str "panel " (when env-state (name env-state)))}
            [:span.tag.release "RELEASE"]
            [:h2.release-definition-name release-definition-name]
            [:hr]
            [:h3.release-number release-number]
            [:hr]
            [:h3.release-env-name env-name]]))
       release-environments))

(defn- generate-build-panel [{:keys [build-definition-name build-definition-id build-number
                                     state commit-message]}]
  [:div {:id (str "build-definition-id-" build-definition-id) :class (str "panel " (when state (name state)))}
   [:span.tag.build "BUILD"]
   [:h2.build-definition-name build-definition-name]
   [:hr]
   [:h3.build-number build-number]
   [:hr]
   [:div.commit-message commit-message]])

(defn- count-total-release-envs [release-info-maps]
  (reduce + (map #(count (:release-environments %)) release-info-maps)))

(defn- panel-dimensions [build-info-maps release-info-maps]
  (let [panel-count (max 1 (+ (count build-info-maps) (count-total-release-envs release-info-maps)))
        max-panels-per-row 4]
    {:panel-width (* 100 (if (<= panel-count max-panels-per-row) (/ panel-count) (/ max-panels-per-row)))
     :panel-height (* 100 (/ (int (Math/ceil (/ panel-count max-panels-per-row)))))}))

(defn generate-build-monitor-html [build-info-maps release-info-maps favicon-path]
  (let [{:keys [panel-width panel-height]} (panel-dimensions build-info-maps release-info-maps)]
    (hiccup/html
     [:head
      [:title "Build Monitor"]
      [:link#favicon {:rel "shortcut icon" :href favicon-path}]
      [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Open+Sans|Roboto"}]
      [:link {:rel "stylesheet" :href
              "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
      [:link {:rel "stylesheet " :href "/style.css" :type "text/css"}]]
     [:style (str ".panel { width:"
                  (if (integer? panel-width) panel-width (format "%.4f" (float panel-width)))
                  "%; height:"
                  (if (integer? panel-height) panel-height (format "%.4f" (float panel-height))) "%; }")]
     [:body
      refresh-icon
      (map generate-build-panel build-info-maps)
      (map generate-release-env-panels release-info-maps)
      [:script {:src "/refresh.js" :defer "defer"}]])))
