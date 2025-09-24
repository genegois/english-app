(ns app.backend.commons.web
  (:require
   [app.backend.utils :as u]
   [java-time :as t]))

(defn wrap-jwt-auth [handler]
  (fn [request]
    (if-let [token (get-in request [:headers "authorization"])]
      (if-let [claims (u/verify-token (second (re-find #"^Bearer (.+)$" token)))]
        (handler (assoc request :identity claims))
        {:status 401
         :body {:error "Invalid or expired token"}})
      {:status 401
       :body {:error "No authorization token provided"}})))

(defn backware
  "Create a json response out of a function, including JWT verification"
  ([fun db request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   ((wrap-jwt-auth
     (fn [req]
       (merge {:status 200
               :headers {"Content-Type" "application/json"}}
              (fun db req))))
    request))
  ([fun db openai request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   ((wrap-jwt-auth
     (fn [req]
       (merge {:status 200
               :headers {"Content-Type" "application/json"}}
              (fun db openai req))))
    request)))

(defn backware-pass ;; ini nge bypass, ini ngebuat jadi return nya json doang
  "Create a json response out of a function"
  ([fun db request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   (let [data (fun db request)]
     (merge {:status  200
             :headers {"Content-Type" "application/json"}}
            data)))
  ([fun db openai request]
   (u/info "=======================================================================")
   (u/info "URI : " (:uri request))
   (merge {:status  200
           :headers {"Content-Type" "application/json"}}
          (fun db openai request))))


(defn frontware-pass
  "Middleware for frontend routes"
  [fun db openai request]
  (u/info "=======================================================================")
  (u/info "URI : " (:uri request))
  (merge {:status  200
          :headers {"Content-Type" "text/html"}}
         (fun db openai request)))

;; Temporary using expiration date for user authorization
(defn frontware
  "Authenticating user bearer token"
  [fun db request]
  (u/info "=======================================================================")
  (u/info "URI : " (:uri request))
  (let [access-token (-> (get-in request [:headers "authorization"])
                         (subs 7))
        token-expired? (-> (u/read-token access-token)
                           (:expired)
                           (t/local-date-time)
                           (t/before? (t/local-date-time)))]
    (if token-expired?
      {:status  401
       :headers {"Content-Type" "application/json"}
       :body    {:status  "error"
                 :message "Access token expired"}}
      (merge {:status  200
              :headers {"Content-Type" "application/json"}}
             (fun db request)))))
