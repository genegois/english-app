(ns app.frontend.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [app.frontend.api :as api]))

;; ------------------------------------------------------------------
;; DB init (also restore localStorage user)
;; ------------------------------------------------------------------
(rf/reg-event-fx
 :init
 (fn [_ _]
   (let [saved (js/localStorage.getItem "user")]
     (if saved
       (let [user (js->clj (js/JSON.parse saved) :keywordize-keys true)
             id   (or (:id user) (:_id user) (:email user))
             user (assoc user :id id)]
         {:db {:user user
               :page :home}
          :dispatch [:fetch-materials]})
       {:db {:user nil :page :login}}))))


(rf/reg-event-db
 :set-user
 (fn [db [_ user-js]]
   (let [user (if (map? user-js) user-js (js->clj user-js :keywordize-keys true))
         id   (or (:id user) (:_id user) (:email user))
         user (assoc user :id id)]
     (assoc db :user user))))

(rf/reg-event-db
 :set-page
 (fn [db [_ page & params]]
   (assoc db :page page :page-params params)))

(rf/reg-event-fx
 :notify
 (fn [_ [_ msg]]
   (js/alert msg)
   {}))

;; ------------------------------------------------------------------
;; API error handler
;; ------------------------------------------------------------------
(rf/reg-event-fx
 :api-failure
 (fn [_ [_ ctx resp]]
   (js/console.error "API gagal (" ctx "):" (clj->js resp))
   (js/alert (str "API gagal (" ctx "): "
                  (or (get resp :message)
                      (get-in resp [:response :message])
                      "Unknown error")))
   {:dispatch [:set-error {:ctx ctx :resp resp}]}))


(rf/reg-event-db
 :set-error
 (fn [db [_ err]]
   (assoc db :last-error err)))

;; ------------------------------------------------------------------
;; Register & Login
;; ------------------------------------------------------------------
(rf/reg-event-fx
 :register
 (fn [_ [_ creds]]
   {:http-xhrio {:method          :post
                 :uri             (api/url "/auth/register")
                 :params          creds
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:register-success]
                 :on-failure      [:api-failure "register"]}}))

(rf/reg-event-fx
 :register-success
 (fn [{:keys [db]} [_ resp]]
   (let [user (if (map? resp) resp (js->clj resp :keywordize-keys true))
         id   (or (:id user) (:_id user) (:email user))
         user (if id (assoc user :id id) user)]
     (js/alert (str "Register sukses! Welkomm " (or (:username user) (:email user)) "!"))
     (if (:id user)
       {:db (assoc db :user user :page :home)
        :dispatch [:fetch-materials]}
       {:db (assoc db :page :login)}))))

(rf/reg-event-fx
 :login
 (fn [_ [_ creds]]
   {:http-xhrio {:method          :post
                 :uri             (api/url "/auth/login")
                 :params          creds
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:login-success]
                 :on-failure      [:api-failure "login"]}}))

(rf/reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ resp]]
   (let [user (if (map? resp) resp (js->clj resp :keywordize-keys true))
         user (assoc user :id (or (:id user) (:_id user) (:email user)))]
     (js/localStorage.setItem "user" (js/JSON.stringify (clj->js user)))
     (js/alert (str "Login sukses, alo " (or (:username user) (:email user)) "!")) 
     {:db (assoc db :user user :page :home)
      :dispatch-n [[:fetch-materials]]})))


;; ------------------------------------------------------------------
;; Materials
;; ------------------------------------------------------------------
(rf/reg-event-fx
 :fetch-materials
 (fn [{:keys [db]} _]
   (let [user (get db :user)
         user-id (or (:id user) (:_id user) (:email user))]
     (if user-id
       {:http-xhrio {:method :get
                     :uri (api/url (str "/materials/user/" user-id))
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:set-materials]
                     :on-failure [:api-failure "fetch-materials"]}}
       (do
         (js/console.warn "fetch-materials: missing user-id" (clj->js user))
         {:db (assoc db :last-error {:ctx :fetch-materials :msg "missing user-id"})})))))

(rf/reg-event-db
 :set-materials
 (fn [db [_ ms]]
   (assoc db :materials (if (sequential? ms) (vec ms) (vec (vals ms))))))

(rf/reg-event-fx
 :fetch-material
 (fn [_ [_ material-id]]
   {:http-xhrio {:method :get
                 :uri (api/url (str "/materials/by-id/" material-id))
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:set-current-material]
                 :on-failure [:api-failure "fetch-material"]}}))

(rf/reg-event-db
 :set-current-material
 (fn [db [_ m]]
   (rf/dispatch [:fetch-prosets (:_id m)])
   (assoc db :current-material m :page :material)))

(rf/reg-event-fx
 :generate-material
 (fn [{:keys [db]} [_ opts]]
   (let [user-id (or (get-in db [:user :id]) (get-in db [:user :_id]) (get-in db [:user :email]))]
     {:http-xhrio {:method :post
                   :uri (api/url "/materials/generate")
                   :params (assoc opts :user-id user-id)
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:generate-material-success]
                   :on-failure [:api-failure "generate-material"]}})))

(rf/reg-event-fx
 :generate-material-success
 (fn [_ [_ _resp]]
   (js/alert "Materi berhasil di-generate & disimpan!")
   {:dispatch [:fetch-materials]}))

;; ------------------------------------------------------------------
;; Prosets
;; ------------------------------------------------------------------
(rf/reg-event-fx
 :fetch-prosets
 (fn [_ [_ material-id]]
   {:http-xhrio {:method :get
                 :uri (api/url (str "/prosets/material/" material-id "/list"))
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:set-prosets]
                 :on-failure [:api-failure "fetch-prosets"]}}))

(rf/reg-event-db
 :set-prosets
 (fn [db [_ ps]]
   (assoc db :prosets (vec ps))))

(rf/reg-event-fx
 :fetch-proset-by-id
 (fn [_ [_ proset-id]]
   {:http-xhrio {:method :get
                 :uri (api/url (str "/prosets/by-id/" proset-id)) ;; ⬅ fix
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:set-current-proset]
                 :on-failure [:api-failure "fetch-proset-by-id"]}}))


(rf/reg-event-db
 :set-current-proset
 (fn [db [_ proset]]
   (assoc db :current-proset proset :page :practice-proset)))


(rf/reg-event-fx
 :generate-prosets
 (fn [_ [_ material-id diff]]
   {:http-xhrio {:method :post
                 :uri (api/url (str "/prosets/material/" material-id "/generate"))
                 :params {:difficulty diff}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:generate-prosets-success]
                 :on-failure [:api-failure "generate-prosets"]}}))


(rf/reg-event-fx
 :generate-prosets-success
 (fn [_ [_ resp]]
   (js/alert "Practice berhasil di-generate!")
   (let [mid (or (:material-id resp)
                 (get resp :material-id)
                 (get resp "material-id"))]
     {:dispatch [:fetch-prosets mid]})))

(rf/reg-event-fx
 :submit-proset
 (fn [_ [_ proset-id answers]]
   {:http-xhrio {:method :post
                 :uri (api/url (str "/prosets/by-id/" proset-id "/submit")) ;; ⬅ ganti
                 :params {:answers answers}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:submit-proset-success]
                 :on-failure [:api-failure "submit-proset"]}}))

(rf/reg-event-fx
 :submit-all-questions
 (fn [_ [_ material-id answers]]
   {:http-xhrio {:method :post
                 :uri (api/url (str "/prosets/material/" material-id "/submit-all"))
                 :params {:answers answers}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:submit-proset-success]
                 :on-failure [:api-failure "submit-all-questions"]}}))


(rf/reg-event-fx
 :fetch-proset
 (fn [_ [_ proset-id]]
   {:http-xhrio {:method :get
                 :uri (api/url (str "/prosets/by-id/" proset-id)) ;; ⬅ ganti
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:set-current-proset]
                 :on-failure [:api-failure "fetch-proset"]}}))

(rf/reg-event-db
 :submit-proset-success
 (fn [db [_ resp]]
   (js/alert (str "Practice selesai! Skor lo: "
                  (get-in resp [:score :correct] 0) "/"
                  (get-in resp [:score :total] 0)))
   (-> db
       (assoc :last-proset-result resp)
       (assoc :page :practice-result))))

(rf/reg-event-fx
 :fetch-last-proset
 (fn [{:keys [db]} _]
   (let [uid (get-in db [:user :id])]
     {:http-xhrio {:method :get
                   :uri (api/url (str "/prosets/user/" uid "/last"))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:set-last-proset-result]
                   :on-failure [:api-failure "fetch-last-proset"]}})))

(rf/reg-event-db
 :set-last-proset-result
 (fn [db [_ resp]]
   (assoc db :last-proset-result resp)))


(rf/reg-event-fx
 :fetch-all-questions
 (fn [{:keys [db]} [_ material-id]]
   {:http-xhrio {:method :get
                 :uri (api/url (str "/prosets/material/" material-id "/all-questions"))
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:set-all-questions]
                 :on-failure [:api-failure "fetch-all-questions"]}}))

(rf/reg-event-fx
 :open-bank-soal
 (fn [_ [_ material-id]]
   {:dispatch [:fetch-prosets material-id]
    :dispatch-later [{:ms 300 :dispatch [:goto-bank-soal]}]}))

(rf/reg-event-db
 :goto-bank-soal
 (fn [db _]
   (assoc db :page :bank-soal)))

(rf/reg-event-db
 :set-all-questions
 (fn [db [_ resp]]
   (assoc db :all-questions resp)))

(rf/reg-event-fx
 :fetch-user-all-questions
 (fn [{:keys [db]} _]
   (let [uid (get-in db [:user :id])]
     {:http-xhrio {:method :get
                   :uri (api/url (str "/prosets/user/" uid "/all-questions"))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:set-user-all-questions]
                   :on-failure [:api-failure "fetch-user-all-questions"]}})))

(rf/reg-event-db
 :set-user-all-questions
 (fn [db [_ resp]]
   (assoc db :user-all-questions resp)))

(rf/reg-event-fx
 :submit-user-all-questions
 (fn [{:keys [db]} [_ answers]]
   (let [uid (get-in db [:user :id])]
     {:http-xhrio {:method :post
                   :uri (api/url (str "/prosets/user/" uid "/all-questions/submit"))
                   :params {:answers answers}
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:submit-proset-success]
                   :on-failure [:api-failure "submit-user-all-questions"]}})))




;; ------------------------------------------------------------------
;; Assessment
;; ------------------------------------------------------------------
(rf/reg-event-fx
 :generate-assessment
 (fn [{:keys [db]} _]
   (let [uid (get-in db [:user :id])]
     {:http-xhrio {:method :post
                   :uri (api/url (str "/assessments/" uid "/generate"))
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:generate-assessment-success]
                   :on-failure [:api-failure "generate-assessment"]}})))

(rf/reg-event-fx
 :generate-assessment-success
 (fn [{:keys [db]} [_ resp]]
   (js/alert "Assessment berhasil digenerate!")
   {:db (assoc db :assessment resp :assessment-started? false :page :assessment-test)}))

(rf/reg-event-fx
 :fetch-assessment
 (fn [{:keys [db]} _]
   (let [user-id (get-in db [:user :id])]
     {:http-xhrio {:method          :get
                   :uri             (api/url (str "/assessments/" user-id))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:set-assessment]
                   :on-failure      [:api-failure "fetch-assessment"]}})))

(rf/reg-event-db
 :set-assessment
 (fn [db [_ a]]
   (assoc db :assessment a :assessment-started? false)))

;; submit assessment
(rf/reg-event-fx
 :submit-assessment
 (fn [{:keys [db]} [_ answers]]
   (let [user-id (get-in db [:user :id])
         assess-id (get-in db [:assessment :_id])]
     {:http-xhrio {:method :post
                   :uri (api/url (str "/assessments/" assess-id "/submit"))
                   :params {:answers answers :user-id user-id}
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:submit-assessment-success]
                   :on-failure [:api-failure "submit-assessment"]}})))

(rf/reg-event-db
 :submit-assessment-success
 (fn [db [_ result]]
   (js/alert "Assessment disubmit! Hasil evaluasi siap.")
   (-> db
       (assoc :assessment result
              :assessment-started? false
              :page :assessment-result))))

(rf/reg-event-fx
 :generate-weak-topics
 (fn [{:keys [db]} _]
   (let [uid (get-in db [:user :id])
         weak (get-in db [:assessment :weak-topics])]
     (if (seq weak)
       {:http-xhrio {:method :post
                     :uri (api/url (str "/materials/generate-weak/" uid))
                     :params {:topics weak}
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:generate-weak-topics-success]
                     :on-failure [:api-failure "generate-weak-topics"]}}
       {:dispatch [:notify "Nggak ada topik lemah buat digenerate"]}))))


(rf/reg-event-fx
 :generate-weak-topics-success
 (fn [{:keys [db]} [_ resp]]
   (js/alert "Materi untuk topik lemah berhasil digenerate!")
   {:db (assoc-in db [:assessment :weak-generated?] true)
    :dispatch [:fetch-materials]}))

;; start assessment (client-side flow)
(rf/reg-event-db
 :start-assessment
 (fn [db _]
   (assoc db :assessment-started? true)))
