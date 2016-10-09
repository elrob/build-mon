(ns build-mon.releases)

(defn- release-not-started? [release] (= (:status release) "notStarted"))
(defn- release-succeeded? [release] (= (:status release) "succeeded"))
(defn- release-in-progress? [release] (= (:status release) "inProgress"))

(defn- get-release-state [env previous-env]
  (cond (release-succeeded? env) :succeeded
        (and (release-in-progress? env) (release-succeeded? previous-env)) :in-progress
        (and (release-in-progress? env) (not (release-succeeded? previous-env))) :in-progress-after-failed
        (release-not-started? env) :not-started
        :default :failed))

(defn- generate-release-environment [previous-environments env]
  (let [env-name (:name env)
        prev-env-with-same-name (some #(when (= env-name (:name %)) %) previous-environments)
        release-state (get-release-state env prev-env-with-same-name)]
    {:env-name env-name
     :state release-state}))

(defn- generate-release-environments [release previous-release]
  (map (partial generate-release-environment (:environments previous-release))
       (:environments release)))

(defn generate-release-info [release previous-release]
  {:release-definition-name (-> release :releaseDefinition :name)
   :release-definition-id (:id release)
   :release-number (:name release)
   :release-environments (generate-release-environments release previous-release)})
