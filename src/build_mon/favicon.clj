(ns build-mon.favicon)

(def states-ordered-worst-first [:failed :in-progress-after-failed :in-progress :succeeded :not-started])

(defn construct-favicon-path [state]
  (str "/favicon_" (name state) ".ico"))

(defn get-release-states [release-info-maps]
  (let [release-envs (flatten (map :release-environments release-info-maps))]
    (map :state release-envs)))

(defn get-build-states [build-info-maps]
  (remove nil? (map :state build-info-maps)))

(defn get-favicon-path [build-info-maps release-info-maps]
  (let [build-states (get-build-states build-info-maps)
        release-states (get-release-states release-info-maps)
        all-states (distinct (concat build-states release-states))
        sorting-map (into {} (map-indexed (fn [idx itm] [itm idx]) states-ordered-worst-first))]
    (construct-favicon-path (first (sort-by sorting-map all-states)))))
