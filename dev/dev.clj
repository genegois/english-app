(ns dev
  (:require
   [com.stuartsierra.component :as component]
   [app.backend.system :as system]
   [app.backend.utils :as u]
   [app.backend.features.english :as english]
   [clojure.tools.namespace.repl :refer [refresh]] 
   [cheshire.core :as json]
   [clj-http.client :as http]
   [immutant.util :as log] 
   [monger.collection :as mc]))

(defonce dev-system (atom nil))

;; create a function that reads config.edn and then get the port
;; looks like only for logging
(defn url
  "Get the port from the config.edn file"
  [path]
  (let [port (-> (u/read-config)
                 (get-in [:server :port]))]
    (str "http://localhost:" port "/api" path)))

;;===== SYSTEM RELATED FUNCTIONS ======

;; Create a function to restart the system

(defn start
  "Starting the webapp"
  []
  (->> (system/create-system)
       (component/start-system)
       (reset! dev-system))
  :system-started)

(defn stop []
  (swap! dev-system component/stop-system))

(defn restart
  []
  (stop)
  (print "Restarting the system ... ")
  ;; (Thread/sleep 100)
  ;; (println "plus/minus 5 minutes.")
  (refresh)
  ;; (u/info "Abis refreshing nih hehe")
  ;; (Thread/sleep 100)
  (log/set-log-level! :ALL)
  (start))

(comment
  (start)
  (refresh) 
  (stop)


  (-> @dev-system :openai)
  (def aid "f9d7bda0989511f095f87e6d5a3d47f1")
  (def dummy-answers
    (mapv (fn [i] {:selected (rand-int 4)}) (range 15)))
  
  (app.backend.features.english/submit-assessment!
   (-> @dev-system :dbase :db) (-> @dev-system :openai)
   aid
   dummy-answers)
  
  (map (fn [q ans]
         (let [correct (:answer-idx q)
               chosen (:selected ans)
               ok? (= correct chosen)]
           (assoc q :user-answer chosen :correct? ok?)))
       (:questions (mc/find-one-as-map (-> @dev-system :dbase :db) "assessments" {:_id aid}))
       dummy-answers)
  
  (:questions (mc/find-one-as-map (-> @dev-system :dbase :db) "assessments" {:_id aid}))

  (app.backend.features.english/fetch-prosets (-> @dev-system :dbase :db) "98660320988511f0adef7e6d5a3d47f1")
  (app.backend.features.english/generate-assessment!  (-> @dev-system :dbase :db) (-> @dev-system :openai) "441bd870989511f095f87e6d5a3d47f1" )
  
  
  



  ;; if db is not seeded with user, it needs to do this
  (app.plumbing.db/init-admin (:dbase @dev-system) "[insert password]")
  (app.plumbing.db/init-trash (:dbase @dev-system))
  ;; note, [mbe todo], make an interface to run this later on
  
  ;; mbe basic test note may 10th 2023
  ;;  - [minor - todo - any] admin change role, no notif if the save action succeed
  ;;   - /dashboard/UserManagement
  ;;
  ;;  - [major - todo - any] connecting to content no content-id validation
  ;;   - /dashboard/SkillManagement
  ;;
  ;;  - [major - todo - any] no content id preview
  ;;   -  /dashboard/ContentManagement
  ;;
  )
