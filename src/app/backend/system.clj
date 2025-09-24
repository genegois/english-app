(ns app.backend.system
  (:require
    [app.backend.plumbing.db :as db]
    [app.backend.plumbing.handler :as http]
    [app.backend.plumbing.openai :as openai]
    [app.backend.plumbing.server :as immut]
    [app.backend.utils :as u]
    [com.stuartsierra.component :as component]))

(defn create-system
  "It creates a system, and return the system, but not started yet"
  []
  (let [{:keys [server-path
                server-port
                server-host
                openai-completion-url
                openai-key

                db-mongo
                db-mongo-uri

                db-mongo-port
                db-mongo-quiet
                db-mongo-debug]} (u/read-config-true-flat)
        server {:port server-port :path server-path :host server-host}
        db-mongo {:port         db-mongo-port
                  :db-mongo-uri db-mongo-uri
                  :db-mongo     db-mongo
                  :quiet        db-mongo-quiet
                  :debug        db-mongo-debug}
        openai-config {:openai-completion openai-completion-url
                       :openai-key        openai-key}]
    (u/info "Preparing the system")
    (u/info "db-mongo config")
    (u/pres db-mongo)
    (u/info "openai-config")
    (u/pres openai-config)
    (component/system-map
      :openai (openai/create-openai-component openai-config)
      :dbase (db/create-database-component db-mongo)
      :handler (component/using (http/create-handler-component) {:dbase  :dbase
                                                                 :openai :openai})
      :server (component/using (immut/create-server-component server) [:handler]))))
