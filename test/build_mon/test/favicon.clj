(ns build-mon.test.favicon
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [build-mon.favicon :as f]))

(fact "there are no missing favicons"
      (let [ico-filenames-in-public-directory (->> (io/resource "public") io/file .list (map str)
                                                   (filter #(string/ends-with? % ".ico")))
            required-favicon-paths (map f/construct-favicon-path f/states-ordered-worst-first)
            required-favicon-filenames (map #(.substring % 1) required-favicon-paths)]
        ico-filenames-in-public-directory => (contains required-favicon-filenames :in-any-order)))

(facts "about get-favicon-path"
       (let [succeeded {:state :succeeded}
             in-progress {:state :in-progress}
             in-progress-after-failed {:state :in-progress-after-failed}
             failed {:state :failed}
             not-started {:state :not-started}
             succeeded-favicon "/favicon_succeeded.ico"
             in-progress-favicon "/favicon_in-progress.ico"
             in-progress-after-failed-favicon "/favicon_in-progress-after-failed.ico"
             failed-favicon "/favicon_failed.ico"]
         (tabular
          (fact "worst state is used for overall favicon"
                (f/get-favicon-path ?build-info-maps ?release-info-maps) => ?favicon-path)

          ?build-info-maps ?release-info-maps ?favicon-path

          [succeeded] [] succeeded-favicon
          [in-progress] [] in-progress-favicon
          [in-progress-after-failed] [] in-progress-after-failed-favicon
          [failed] [] failed-favicon
          [succeeded in-progress] [] in-progress-favicon
          [in-progress succeeded] [] in-progress-favicon
          [in-progress in-progress-after-failed] [] in-progress-after-failed-favicon
          [in-progress-after-failed failed] [] failed-favicon
          [succeeded in-progress in-progress-after-failed failed] [] failed-favicon

          [] [{:release-environments [in-progress]}] in-progress-favicon
          [] [{:release-environments [succeeded not-started]}] succeeded-favicon
          [] [{:release-environments [succeeded]}
              {:release-environments [in-progress]}] in-progress-favicon
          [] [{:release-environments [in-progress]}
              {:release-environments [succeeded]}] in-progress-favicon

          [succeeded] [{:release-environments [in-progress]}] in-progress-favicon
          [in-progress] [{:release-environments [succeeded]}] in-progress-favicon)))
