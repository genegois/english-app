(ns app.backend.bp-test.ctrl
  (:require [app.backend.bp-test.logic :as logic]))

(defn test-get
  [db-comp openai-comp req]
  (if-let [result (logic/test-get db-comp openai-comp)]
    {:status 200
     :body {:status  "ok"
            :message "Test get is running fine"
            :data    result}}
    {:status 500
     :body {:status  "error"
            :message "Test get is not running fine because db-comp or openai-comp is missing"}}))

