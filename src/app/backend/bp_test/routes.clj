(ns app.backend.bp-test.routes
  (:require [app.backend.bp-test.ctrl :as ctrl]))

(defn api-check
  "Helper function for testing api"
  [request]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    {:status  "ok"
             :message "API test is running fine"}})

(defn test-routes
  [db-comp openai-comp midware]
  ["/test"
   ["" {:get api-check}]
   ["/test-get" {:get (partial midware ctrl/test-get db-comp openai-comp)}]
   ;["/test-post" {:post (partial midware ctrl/test-post db-comp openai-comp)}] ;ini tinggal tambahin routes lain kyk gini
   ])