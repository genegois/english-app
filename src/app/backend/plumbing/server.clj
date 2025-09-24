(ns app.backend.plumbing.server
  (:require [com.stuartsierra.component :as component]
            [immutant.web :as web]))

(defrecord Server [config handler]
  component/Lifecycle
  (start [this] 
    (let [server (web/run (:handler handler) config)] 
      (println "Server started" (:port config)) 
      (assoc this :server server)))
  (stop [this]
    (when-let [server (:server this)]
      (web/stop server))
    (dissoc this :server)))

(defn create-server-component [config]
  (map->Server {:config config}))
