(ns build-mon.test.core
  (:require [clojure.test :refer :all]
            [build-mon.core :as c]))

(deftest determine-background-colour
  (let [succeeded-build {:result "succeeded"}
        in-progress-build {:result nil :status "inProgress"}
        failed-build {:result "failed"}]
    (is (= (c/determine-background-colour succeeded-build :anything) "green"))
    (is (= (c/determine-background-colour failed-build :anything) "red"))
    (is (= (c/determine-background-colour in-progress-build succeeded-build) "yellow"))
    (is (= (c/determine-background-colour in-progress-build failed-build) "orange"))))
