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
                   (let [new-user {:_id (u/uuid)
                                   :email email
                                   :username username}]
                     (mc/insert-and-return (:db db) "users" new-user)
                     {:status 200 :body new-user}))))}]
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
               (let [{:keys [user-id topic]} (:body req)]
                 (if (empty? topic)
                   {:status 400
                    :body {:status "error" :message "Isi dulu topicnya boss"}}
                   {:status 200
                    :body (english/generate-material! (:db db) openai user-id topic)})))}]
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
                   (let [m (english/generate-material! (:db db) openai uid t)]
                     (english/generate-proset! (:db db) openai (:_id m) "medium")
                     (english/generate-proset! (:db db) openai (:_id m) "hard")))
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
              (let [mid (get-in req [:path-params :material-id])
                    prosets (mc/find-maps (:db db) "prosets" {:material-id mid})]
                {:status 200 :body prosets}))}]
     ["/material/:material-id/all-questions"
      {:get (fn [req]
              (let [mid (get-in req [:path-params :material-id])
                    prosets (mc/find-maps (:db db) "prosets" {:material-id mid})
                    all-problems (mapcat :problems prosets)
                    randomized (english/shuffle-questions all-problems)]
                {:status 200
                 :body {:material-id mid
                        :problems randomized}}))}]
     ["/material/:material-id/submit-all"
      {:post (fn [req]
               (let [answers (get-in req [:body :answers])
                     problems (get-in req [:body :problems])
                     result (english/grade-problems problems answers)]
                 {:status 200 :body result}))}]

     ;; single proset
     ["/by-id/:proset-id"
      {:get (fn [req]
              (let [pid (get-in req [:path-params :proset-id])
                    proset (mc/find-one-as-map (:db db) "prosets" {:_id pid})
                    randomized (update proset :problems english/shuffle-questions)]
                {:status 200 :body randomized}))}]
     ["/by-id/:proset-id/submit"
      {:post (fn [req]
               (let [answers (get-in req [:body :answers])
                     problems (get-in req [:body :problems])
                     result (english/grade-problems problems answers)]
                 {:status 200 :body result}))}] 
     ["/by-id/:proset-id/delete" 
      {:delete (fn [req]
                 (let [pid (get-in req [:path-params :proset-id])
                       result (mc/remove (:db db) "prosets" {:_id pid})]
                   (if result
                    {:status 200 :body {:status "success" :message "Proset dihapus"}}
                     {:status 404 :body {:status "error" :message "Proset gak ketemu"}})))}]

     ;; fetch all user questions
     ["/user/:user-id/all-questions"
      {:get (fn [req]
              (let [uid (get-in req [:path-params :user-id])
                    materials (mc/find-maps (:db db) "materials" {:user-id uid})
                    mids (map :_id materials)
                    prosets (mapcat #(mc/find-maps (:db db) "prosets" {:material-id %}) mids)
                    all-problems (mapcat :problems prosets)
                    randomized (english/shuffle-questions all-problems)]
                {:status 200
                 :body {:user-id uid
                        :problems randomized}}))}]
     ["/user/:user-id/all-questions/submit"
      {:post (fn [req]
               (let [answers (get-in req [:body :answers])
                     problems (get-in req [:body :problems])
                     result (english/grade-problems problems answers)]
                 {:status 200 :body result}))}]
     ;; custom prosets
    ["/custom"
     ["/generate"
      {:post (fn [req]
               (let [uid (get-in req [:body :user-id])
                     material-ids (get-in req [:body :material-ids])
                     title (get-in req [:body :title])]
                 (if (empty? material-ids)
                   {:status 400
                    :body {:status "error" :message "Material lo kosong, kocak"}}
                   {:status 200
                    :body (english/generate-custom-proset! (:db db) uid material-ids title)})))}]
     ["/user/:user-id"
      {:get (fn [req]
              (let [uid (get-in req [:path-params :user-id])]
                {:status 200
                 :body (vec
                        (mc/find-maps (:db db)
                                      "prosets-custom"
                                      {:user-id uid}))}))}]

     ["/id/:custom-id"
      {:get (fn [req]
              (let [cid (get-in req [:path-params :custom-id])
                    proset (mc/find-one-as-map (:db db) "prosets-custom" {:_id cid})
                    randomized (update proset :problems english/shuffle-questions)]
                {:status 200 :body randomized}))}]
     ["/id/:custom-id/submit"
      {:post (fn [req]
               (let [answers (get-in req [:body :answers])
                     problems (get-in req [:body :problems])
                     result (english/grade-problems problems answers)]
                 {:status 200 :body result}))}]
     ["/id/:custom-id/delete"
      {:delete (fn [req]
                 (let [cid (get-in req [:path-params :custom-id])
                       result (mc/remove (:db db) "prosets-custom" {:_id cid})]
                   (if result
                    {:status 200
                     :body {:status "success"
                            :message "Latihan berhasil dihapus"}}
                     {:status 404
                      :body {:status "error"
                             :message "Latihan gak ditemukan"}})))}]]]

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
