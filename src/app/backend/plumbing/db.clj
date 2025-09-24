(ns app.backend.plumbing.db
  (:require
   [com.stuartsierra.component :as component]
   [monger.core :as mg]
   [monger.collection :as mc]
   [app.backend.utils :as u])
  (:import [org.bson.types ObjectId]))

;; --------------------- Component ---------------------
(defrecord Dbase [db-mongo-config]
  component/Lifecycle
  (start [this]
    (u/info "Starting the database component with the following key")
    (u/pres
     #_db-mongo-config
     (keys db-mongo-config))
    (let [conn-db (mg/connect (assoc db-mongo-config :uri
                                     (:db-mongo-uri db-mongo-config)))
          db (mg/get-db conn-db (:db-mongo db-mongo-config))

          ;conn-universal (mg/connect (assoc db-mongo-config :uri
          ;                                   (:uri-universal db-mongo-config)))
          ;db-universal (mg/get-db conn-universal (:db-universal db-mongo-config))
          ;;add more db and its conn here

          ;scheduler-running? (atom true)
          ]
      (u/info "Starting the database and the dbref")
      (merge this {:conn-db      conn-db
                   :db           db
                   ;:scheduler-running?      scheduler-running?
                   })))
  (stop [this]
    (mg/disconnect (:conn-db this))
    (u/info "Database stopped")
    (dissoc this :conn-db :db)
    ;(reset! (:scheduler-running? this) false)
    ;(u/info "Scheduler stopped")
    ))

(defn create-database-component [db-mongo-config]
  (map->Dbase {:db-mongo-config db-mongo-config}))








