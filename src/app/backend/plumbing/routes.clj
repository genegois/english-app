(ns app.backend.plumbing.routes
  (:require
   [reitit.ring :as ring]
   [monger.collection :as mc]
   [app.backend.features.english :as english]
   [app.backend.utils :as u]))

(defn api-check [_ _]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body {:status "ok" :message "API is alive"}})

(defn api [db openai _]
  ["/api"
   ["/v1"

    ;; --- AUTH ---
    ["/auth"
     ["/register"
      {:post (fn [req]
               (let [{:keys [email username]} (:body req)
                     existing (mc/find-one-as-map (:db db) "users" {:email email})]
                 (if existing
                   {:status 400 :body {:status "error" :message "Email udah kedaftar gan"}}
                   (do (mc/insert-and-return (:db db) "users"
                                             {:_id (u/uuid)
                                              :email email
                                              :username username})
                       {:status 200 :body {:email email :username username}}))))}]
     ["/login"
      {:post (fn [req]
               (let [{:keys [email]} (:body req)
                     user (mc/find-one-as-map (:db db) "users" {:email email})]
                 (if user
                   {:status 200 :body user}
                   {:status 404 :body {:status "error" :message "Belom daftar gan"}})))}]]

    ;; --- MATERIALS ---
    ["/materials"
     ["/generate"
      {:post (fn [req]
               (let [{:keys [user-id topic difficulty]} (:body req)]
                 {:status 200
                  :body (english/generate-material! (:db db) openai user-id topic difficulty)}))}]
     ["/user/:user-id"
      {:get (fn [req]
              (let [uid (get-in req [:path-params :user-id])]
                {:status 200
                 :body (english/fetch-materials (:db db) uid)}))}]
     ["/by-id/:material-id"
      {:get (fn [req]
              (let [mid (get-in req [:path-params :material-id])]
                {:status 200
                 :body (english/fetch-material-by-id (:db db) mid)}))}]
     ["/generate-weak/:user-id"
      {:post (fn [req]
               (let [uid   (get-in req [:path-params :user-id])
                     topics (get-in req [:body :topics])]
                 (doseq [t topics]
                   (let [m (english/generate-material! (:db db) openai uid t "medium")]
                     (english/generate-proset! (:db db) openai (:_id m) "medium")))
                 {:status 200
                  :body {:status "ok" :generated topics}}))}]]

    ;; --- PROSETS ---
    ["/prosets"
     ["/material/:material-id/generate"
      {:post (fn [req]
               (let [mid (get-in req [:path-params :material-id])
                     difficulty (get-in req [:body :difficulty] "medium")]
                 {:status 200
                  :body (english/generate-proset! (:db db) openai mid difficulty)}))}]
     ["/material/:material-id/list"
      {:get (fn [req]
              (let [mid (get-in req [:path-params :material-id])]
                {:status 200
                 :body (english/fetch-prosets (:db db) mid)}))}]
     ["/material/:material-id/bank"
      {:get (fn [req]
              (let [mid (get-in req [:path-params :material-id])
                    prosets (mc/find-maps (:db db) "prosets" {:material-id mid})]
                {:status 200 :body prosets}))}]
     ["/material/:material-id/all-questions"
      {:get (fn [req]
              (let [mid (get-in req [:path-params :material-id])
                    prosets (mc/find-maps (:db db) "prosets" {:material-id mid})
                    all-problems (mapcat :problems prosets)]
                {:status 200
                 :body {:material-id mid
                        :problems all-problems}}))}]
     ["/material/:material-id/submit-all"
  {:post (fn [req]
           (let [mid (get-in req [:path-params :material-id])
                 answers (get-in req [:body :answers])
                 prosets (mc/find-maps (:db db) "prosets" {:material-id mid})
                 all-problems (mapcat :problems prosets)
                 result (english/grade-problems all-problems answers)]
             {:status 200 :body result}))}]

     ;; fetch last proset by user
     ["/user/:user-id/last"
      {:get (fn [req]
              (let [uid (get-in req [:path-params :user-id])
                    p (first (mc/find-maps (:db db) "prosets"
                                           {:user-id uid}
                                           {:sort {:created-at -1} :limit 1}))]
                (if p
                  {:status 200 :body p}
                  {:status 404 :body {:status "error" :message "No last proset found"}})))}]
     ["/user/:user-id/all-questions/submit"
      {:post (fn [req]
               (let [uid (get-in req [:path-params :user-id])
                     answers (get-in req [:body :answers])
                     materials (mc/find-maps (:db db) "materials" {:user-id uid})
                     mids (map :_id materials)
                     prosets (mapcat #(mc/find-maps (:db db) "prosets" {:material-id %}) mids)
                     all-problems (mapcat :problems prosets)
                     result (english/grade-problems all-problems answers)]
                 {:status 200 :body result}))}]

     ;; single proset
     ["/by-id/:proset-id"
      {:get (fn [req]
              (let [pid (get-in req [:path-params :proset-id])]
                {:status 200
                 :body (mc/find-one-as-map (:db db) "prosets" {:_id pid})}))}]
     ["/by-id/:proset-id/submit"
      {:post (fn [req]
               (let [pid (get-in req [:path-params :proset-id])
                     answers (get-in req [:body :answers])]
                 {:status 200 :body (english/submit-proset! (:db db) pid answers)}))}]

     ;; fetch all user questions
     ["/user/:user-id/all-questions"
      {:get (fn [req]
              (let [uid (get-in req [:path-params :user-id])
                    materials (mc/find-maps (:db db) "materials" {:user-id uid})
                    mids (map :_id materials)
                    prosets (mapcat #(mc/find-maps (:db db) "prosets" {:material-id %}) mids)
                    all-problems (mapcat :problems prosets)]
                {:status 200
                 :body {:user-id uid
                        :problems all-problems}}))}]]


    ;; --- ASSESSMENTS ---
    ["/assessments"
     ["/:user-id/generate"
      {:post (fn [req]
               (let [uid (get-in req [:path-params :user-id])]
                 {:status 200
                  :body (english/generate-assessment! (:db db) openai uid)}))}]
     ["/:user-id"
      {:get (fn [req]
              (let [uid (get-in req [:path-params :user-id])]
                {:status 200
                 :body (mc/find-one-as-map (:db db) "assessments" {:user-id uid})}))}]
     ["/:assess-id/submit"
      {:post (fn [req]
               (let [aid (get-in req [:path-params :assess-id])
                     answers (get-in req [:body :answers])]
                 {:status 200
                  :body (english/submit-assessment! (:db db) openai aid answers)}))}]]]])

(defn create-routes [db openai]
  (ring/router
   [["/" {:get (partial api-check db)}]
    (api db openai nil)]))
