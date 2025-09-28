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


  (app.backend.features.english/fetch-prosets (-> @dev-system :dbase :db) "98660320988511f0adef7e6d5a3d47f1")
  (app.backend.features.english/generate-assessment!  (-> @dev-system :dbase :db) (-> @dev-system :openai) "441bd870989511f095f87e6d5a3d47f1" )
  
  ;; if db is not seeded with user, it needs to do this
  (app.plumbing.db/init-admin (:dbase @dev-system) "[insert password]")
  (app.plumbing.db/init-trash (:dbase @dev-system)))
