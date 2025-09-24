(ns app.backend.plumbing.handler
  (:require
   [app.backend.utils :as u]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [app.backend.plumbing.routes :as routes]
   [reitit.ring :as ring]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [jumblerg.middleware.cors :as jcors]))

(defn not-found-handler [_]
  {:status 404
   :body   {:status  "error"
            :message "Not Found"}})

(defn create-handler [db openai]
  (-> (routes/create-routes db openai)
      (ring/ring-handler
       (ring/create-default-handler
        {:not-found not-found-handler}))
      (jcors/wrap-cors #".*")
      wrap-params
      ;(wrap-multipart-params
      ;  {:store (mp/temp-file-store
      ;            {:filename-fn (fn [component filename]
      ;                            (u/info "Debug Handler - Component:" component)
      ;                            (u/info "Debug Handler - Filename:" filename)
      ;                            filename)})}) ;tried to keep the original file instead of ring's temp's file, but this dont ducking work idk why
      (wrap-json-body {:keywords? true :bigdecimals? true})
      wrap-cookies
      wrap-session
      wrap-json-response
      wrap-stacktrace
      ;; https://github.com/steffan-westcott/clj-otel/blob/master/doc/guides.adoc#work-with-http-client-and-server-spans
      ;; looks like wrap-route is used when having more spesific use case
      ;; trace-http/wrap-route
      ;; (trace-http/wrap-server-span {:create-span? false})
      ;; false, we use agent for creating span
      ;; trace-http/wrap-exception-event
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

(defrecord Handler [dbase openai]
  component/Lifecycle
  (start [this]
    (assoc this :handler (create-handler dbase openai)))
  (stop [this]
    this))

(defn create-handler-component []
  (map->Handler {}))
