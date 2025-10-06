(ns app.frontend.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Helper
;; -----------------------------------------------------------------------------
(defn input-field [val-atom placeholder type]
  [:input {:type type
           :class "input-field"
           :value (or @val-atom "")
           :placeholder placeholder
           :on-change #(reset! val-atom (.. % -target -value))}])

;; -----------------------------------------------------------------------------
;; AUTH
;; -----------------------------------------------------------------------------
(defn auth-page []
  (let [login-email    (r/atom "")
        reg-email      (r/atom "")
        reg-username   (r/atom "")]
    (fn []
      [:div.container
       [:h1.page-title "English Learning nih, hehe"]
       [:p.tagline "Register dulu brok, klo udah tinggal login dah!"]
       ;; REGISTER PANEL
       [:div.panel
        [:h2 "Register"]
        [input-field reg-email "Email" "email"]
        [input-field reg-username "Username" "text"]
        [:div
         [:button.btn.btn-success
          {:on-click #(rf/dispatch
                       [:register {:email @reg-email
                                   :username @reg-username}])}
          "Register"]]]
       ;; LOGIN PANEL
       [:div.panel
        [:h2 "Login"]
        [input-field login-email "Email" "email"]
        [:div
         [:button.btn.btn-primary
          {:on-click #(rf/dispatch [:login {:email @login-email}])}
          "Login"]]]])))

;; -----------------------------------------------------------------------------
;; HOME
;; -----------------------------------------------------------------------------
(defn home-page []
  [:div.container
   [:h1.page-title "English Learning nih hehe"]
   [:h2 "Home"]
   [:div.btn-group
    [:button.btn {:on-click #(set! (.-hash js/location) "#generate")} "Generate Manual"]
    [:button.btn {:on-click #(set! (.-hash js/location) "#materials")} "List Materi"]
    [:button.btn {:on-click #(set! (.-hash js/location) "#assessment")} "Assessment"]]])

;; -----------------------------------------------------------------------------
;; MATERIALS
;; -----------------------------------------------------------------------------
(defn materials-page []
  (fn []
    (let [materials @(rf/subscribe [:materials])] 
      [:div.container
       [:h2.page-title "List Materi"]

       ;; tombol refresh
       [:div.row
        [:button.btn.btn-secondary
         {:on-click #(rf/dispatch [:fetch-materials])}
         "Refresh"]
        [:button.btn.btn-secondary
         {:on-click #(set! (.-hash js/location) "#/")}
         "Balik ke halaman utama"]]

       ;; daftar materi
       (if (seq materials)
         [:ul.list
          (for [m materials]
            ^{:key (str (:_id m))}
            [:li.list-item
             [:a.link
              {:href (str "#material/" (:_id m))
               :on-click (fn [e]
                           (.preventDefault e)
                           (rf/dispatch [:fetch-material (:_id m)]))}
              (str (or (:topic m) "Untitled"))]])]
         [:p.small "Belum ada materi."])
       
       [:button.btn.btn-secondary
        {:style {:margin-top "1em"}
         :on-click #(set! (.-hash js/location) "#custom-proset")}
        "Buat Latihan Gabungan Custom"]

       ;; latihan gabungan semua materi
       [:div.panel {:style {:margin-top "2em"}}
        [:h3.subtle "Latihan Gabungan Semua Materi"]
        [:button.btn.btn-primary
         {:on-click #(do
                       (rf/dispatch [:fetch-user-all-questions])
                       (set! (.-hash js/location) "#practice-all-user"))}
         "Tes Semua Soal dari Semua Materi"]]])))

(defn text-from [node lang]
  (cond
    (nil? node) nil
    (string? node) node
    (map? node) (or (get node lang) (get node :en) (get node "en") (get node :id) (get node "id") (pr-str node))
    :else (pr-str node)))

(defn render-multi 
  "items can be vector of objects or single object; return hiccup list or nil"
  [items lang] 
  (cond
    (nil? items) nil
    (and (sequential? items) (seq items))
    [:ul (for [it items] ^{:key (text-from it lang)} [:li (text-from it lang)])]
    (map? items)
    [:div 
     (for [[k v] items] 
       ^{:key (str k)} 
       [:p (str (str/upper-case (name k)) ": " (text-from v lang))])]
    :else [:div (text-from items lang)]))

(defn lang-block [title node]
  [:div
   [:p [:strong title]]
   [:ul.lang-list
    [:li [:strong "EN: "] (text-from node :en)]
    [:li [:strong "ID: "] (text-from node :id)]]])

(defn material-page []
  (fn []
    (let [m @(rf/subscribe [:current-material])
          ps @(rf/subscribe [:prosets])] 
      [:div.container
       [:h2.page-title "Detail Materi"]

       (if m
         [:div
          [:p [:strong "Topic: "] (or (:topic m) "—")]

          ;; definisi / usage / importance / tips
          [:div.panel
           (lang-block "Definition" (get-in m [:content :definition]))
           (lang-block "Usage" (get-in m [:content :usage]))
           (lang-block "Importance" (get-in m [:content :importance]))
           (lang-block "Tips" (get-in m [:content :tips]))]

          ;; common mistakes
          [:div.panel
           [:h3.subtle "Common mistakes"]
           (render-multi (get-in m [:content :common-mistakes :en]) :en)
           [:h3.subtle "Kesalahan umum"]
           (render-multi (get-in m [:content :common-mistakes :id]) :id)]

          ;; examples
          [:div.panel
           [:h3.subtle "Examples"]
           (let [ex (get-in m [:content :examples])]
             [:ul.material-list
              [:li (str "Positive EN: " (text-from (get-in ex [:positive]) :en))]
              [:li (str "Positive ID: " (text-from (get-in ex [:positive]) :id))]
              [:li (str "Negative EN: " (text-from (get-in ex [:negative]) :en))]
              [:li (str "Negative ID: " (text-from (get-in ex [:negative]) :id))]])]

          ;; practice
          [:div.panel
           [:h3.subtle "Practice"]
           (if (seq ps)
             [:div.btn-group
              [:button.btn.btn-primary
               {:on-click #(set! (.-hash js/location) (str "#practice/" (:_id m)))}
               "Menu Practice"]]
             [:div
              [:p.small "Practice belum ada boss. Lo harusnya gak lihat ini sih"]])]]
         [:p.small "Materi belum dimuat. Klik dari List dulu."])])))

;; -----------------------------------------------------------------------------
;; GENERATE
;; -----------------------------------------------------------------------------
(defn generate-page []
  (let [topic (r/atom "")]
    (fn []
      [:div.container
       [:h2.page-title "Generate Materi"]
       [input-field topic "Topik (misal Vocabulary atau lo bisa bacot kasih konteks mau belajar apaan)" "text"] 
       [:div
        [:button.btn.btn-success
         {:on-click #(rf/dispatch [:generate-material {:topic @topic}])}
         "Generate"]]
       [:p.small
        [:a {:href "#materials"} "← Lihat List Materi"]]])))

;; -----------------------------------------------------------------------------
;; ASSESSMENT
;; -----------------------------------------------------------------------------
(def lockout-ms (* 60 60 1000))

(defn assessment-page []
  (let [assessment @(rf/subscribe [:assessment])
        submitted? @(rf/subscribe [:assessment-submitted?])
        last-ts    @(rf/subscribe [:last-assessment-timestamp])
        now        (.now js/Date)]
    [:div.container
     [:h2.page-title "Assessment"]

     (cond
       ;; Lockout aktif (udah submit dan belum lewat 1 jam)
       (and submitted? last-ts (< (- now last-ts) lockout-ms))
       [:div
        [:p "Lo udah ngerjain assessment, coba sejam lagi bro"]
        [:button.btn.btn-secondary
         {:on-click #(set! (.-hash js/location) "#assessment-result")}
         "Lihat Hasil Assessment"]]

       ;; Kalau assessment udah ada tapi belum dikerjain
       (and assessment (not submitted?))
       [:div.btn-group
        [:button.btn.btn-secondary
         {:on-click #(do
                       (rf/dispatch [:fetch-assessment])
                       (set! (.-hash js/location) "#assessment-test"))}
         "Mulai Tes!"]]

       ;; Kalau belum ada assessment → kasih opsi generate
       :else
       [:div
       [:p.desc
        "Assessment ini tuh tes menyeluruh buat ngecek kemampuan lo "
        "dalam grammar, vocabulary, dan reading. Anggep aja ini ‘ultimate test’ "
        "buat tau level lo sekarang."] 
        [:div.btn-group
         [:button.btn.btn-primary
          {:on-click #(rf/dispatch [:generate-assessment])}
          "Generate Assessment"]]
        ])]))

(defn assessment-test-page []
  (fn []
    (let [a @(rf/subscribe [:assessment])
          answers (r/atom {})] 
      [:div.container
       [:h2.page-title "Assessment Test"]
       (if (seq (:questions a)) 
         [:div
          [:ol
           (for [[idx q] (map-indexed vector (:questions a))]
             ^{:key (str "assess-q-" idx)}
             [:li.question
              [:div.problem (:problem q)]
              [:ul.options
               (for [[i opt] (map-indexed vector (:choices q))]
                 ^{:key (str "assess-opt-" idx "-" i)}
                 [:li.option
                  [:label
                   [:input {:type "radio"
                            :name (str "assess-q" idx)
                            :value i
                            :on-change #(swap! answers assoc idx i)}]
                   [:span (str (char (+ 65 i)) ". " opt)]]])]])]
          [:button.btn.btn-primary
           {:on-click #(rf/dispatch
                        [:submit-assessment
                         (mapv (fn [i]
                                 {:selected (get @answers i nil)})
                               (range (count (:questions a))))])}
           "Submit Jawaban"]]

         [:p "Belum ada assessment gan, generate assessment dulu gih!"])])))

(defn assessment-result-page []
  (let [result @(rf/subscribe [:assessment])]
    [:div.container
     [:h2.page-title "Hasil Assessment"]
     (if result
       [:div
        [:p.score
         (str "Skor: "
              (get-in result [:score :correct] 0)
              " / "
              (get-in result [:score :total] 0))]

        [:ol
         (for [[idx q] (map-indexed vector (:questions result))]
           ^{:key (str "ass-res-" idx)}
           [:li.question
            [:div.problem (:problem q)]
            [:ul.options
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "ass-opt-" idx "-" i)}
               [:li.option
                (cond
                  (= i (:user-answer q))
                  [:span.user-answer {:class (if (:correct? q) "correct" "wrong")}
                   (str (char (+ 65 i)) ". " choice)
                   (if (:correct? q) " ✓" " ✗")]

                  (= i (:answer-idx q))
                  [:span.correct-answer
                   (str (char (+ 65 i)) ". " choice " (correct)")]

                  :else (str (char (+ 65 i)) ". " choice))])]

            [:p.explanation
             [:em "Explanation: "] (get-in q [:explanation :en])]
            [:p.explanation
             [:em "Penjelasan: "] (get-in q [:explanation :id])]])]

        (let [weak (or (:weak-topics result) [])
              already? (:weak-generated? result)]
          [:div
           [:h3 "Topik Lemah"]
           (if (seq weak)
             [:div
              [:ul (for [t weak] ^{:key t} [:li t])]
              (if-not already?
                ;; case: belum generate
                [:div
                 [:button.btn.btn-accent
                  {:on-click #(rf/dispatch [:generate-weak-topics])}
                  "Generate materi buat topik lemah"]
                 [:div.btn-group
                  [:button.btn.btn-secondary
                   {:on-click #(set! (.-hash js/location) "#/")}
                   "Balik ke halaman utama"]
                  [:button.btn.btn-primary
                   {:on-click #(set! (.-hash js/location) "#materials")}
                   "Lihat List Materi lo"]]]
                ;; case: sudah generate
                [:div
                 [:ul (for [t weak] ^{:key t} [:li t])]
                 [:p "Materi lemah sudah digenerate!"]
                 [:div.btn-group
                  [:button.btn.btn-secondary
                   {:on-click #(set! (.-hash js/location) "#/")}
                   "Balik ke halaman utama"]
                  [:button.btn.btn-primary
                   {:on-click #(set! (.-hash js/location) "#materials")}
                   "Lihat List Materi lo"]]])]
             [:p "Ngeri kali brok! Nggak ada topik lemah nih, GG!"])])]

       [:p "Belum ada hasil assessment. Coba kerjain dulu gih tesnya!"])]))

;; -----------------------------------------------------------------------------
;; PRACTICE
;; -----------------------------------------------------------------------------
(defn custom-proset-page []
  ;; ⬇️ state lokal dideklarasi di luar fn render
  (let [selected (r/atom #{})
        title (r/atom "")]
    (fn []  ;; render function
      (let [materials @(rf/subscribe [:materials])
            custom @(rf/subscribe [:custom-proset])
            user @(rf/subscribe [:user])
            custom-list @(rf/subscribe [:user-custom-prosets])
            loading? @(rf/subscribe [:loading-custom-prosets?])]
        [:div.container
         [:h2.page-title "Buat Latihan Gabungan"]

         ;; input title (gaya generate-page)
         [input-field title "Judul (opsional) — misal: Gabungan Love + Nurture" "text"]

         ;; Checkbox list materi
         [:div {:style {:margin-top "1em"}}
          [:h4 "Pilih Materi lo:"]
          (doall
           (for [m materials]
             ^{:key (:_id m)}
             [:div
              [:input {:type "checkbox"
                       :checked (contains? @selected (:_id m))
                       :on-change #(swap! selected
                                          (fn [s]
                                            (if (contains? s (:_id m))
                                              (disj s (:_id m))
                                              (conj s (:_id m)))))}]
              [:span {:style {:margin-left "0.5em"}}
               (or (get-in m [:content :topic])
                   (:topic m)
                   "Untitled")]]))]

         ;; Tombol generate
         [:div {:style {:margin-top "1.5em"}}
          [:button.btn.btn-primary
           {:on-click #(rf/dispatch [:generate-custom-proset (vec @selected) @title])}
           "Generate Gabungan"]]

         ;; Hasil tampil (baru generate)
         (when custom
           [:div {:style {:margin-top "2em"
                          :padding "1em"
                          :border "1px solid #ddd"
                          :border-radius "6px"
                          :background "#f9f9f9"}}
            [:h3 "✅ Latihan berhasil dibuat!"]
            [:p (str "Judul: " (:topic custom))]
            [:p (str "Jumlah Soal: " (count (:problems custom)))]
            [:button.btn.btn-secondary
             {:on-click #(do
                           (rf/dispatch [:fetch-custom-proset-by-id (:_id custom)])
                           (set! (.-hash js/location)
                                 (str "#practice-custom/" (:_id custom))))}
             "Mulai Latihan"]])

         ;; Divider
         [:hr {:style {:margin "2em 0"}}]

         ;; List custom proset user
         [:h3 "Latihan Gabungan lo"]
         [:div {:style {:margin-bottom "1em"}}
          [:button.btn.btn-outline-primary
           {:on-click #(rf/dispatch
                        [:fetch-user-custom-prosets
                         (or (:id user) (:_id user) (:email user))])}
           (if loading? "Loading..." "🔄 Refresh List")]]

         (cond
           loading? [:p "Sedang memuat..."]

           (seq custom-list)
           [:div.custom-list
            (doall
             (for [c (reverse custom-list)]
               ^{:key (:_id c)}
               [:div.custom-item
                {:style {:border "1px solid #ddd"
                         :padding "0.75em"
                         :margin-bottom "0.5em"
                         :border-radius "6px"}}
                [:div {:style {:display "flex"
                               :justify-content "space-between"
                               :align-items "center"}}
                 [:div
                  [:strong (or (:topic c) "Untitled")]
                  [:div {:style {:font-size "0.85em" :color "#666"}}
                   (str "Soal: " (count (:problems c)))]]
                 [:div
                  [:button.btn.btn-sm.btn-secondary
                   {:on-click #(do
                                 (rf/dispatch [:fetch-custom-proset-by-id (:_id c)])
                                 (set! (.-hash js/location)
                                       (str "#practice-custom/" (:_id c))))}
                   "Mulai"]]]]))]

           :else
           [:p "Belum ada latihan gabungan yang lo buat."])]))))

(defn custom-proset-test-page []
  (let [p @(rf/subscribe [:custom-current-proset])
        answers (r/atom {})]
    [:div.container
     [:h2.page-title (or (:topic p) "Latihan Gabungan")]
     (if (seq (:problems p))
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems p))]
           ^{:key (str "custom-q-" idx)}
           [:li.question
            [:div.problem (:problem q)]
            [:ul.options
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "custom-opt-" idx "-" i)}
               [:li.option
                [:label
                 [:input {:type "radio"
                          :name (str "custom-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 [:span {:style {:margin-left "0.6em"}}
                  (str (char (+ 65 i)) ". " (or (:text choice) choice))]]])]])]
        [:div {:style {:margin-top "1em"}}
         [:button.btn.btn-success
          {:on-click #(rf/dispatch
                       [:submit-custom-proset
                        (:_id p)
                        (mapv (fn [i]
                                {:selected (get @answers i nil)})
                              (range (count (:problems p))))])}
          "Submit Jawaban"]]]
       [:p "Soal kosong bro, coba generate dulu."])]))


(defn practice-page [material-id]
  (let [difficulty (r/atom "easy")]
    (fn []
      [:div.container
       [:h2.page-title "Practice Menu"]
       (if (some? material-id)
         [:div  
          [:div.btn-group
           [:button.btn.btn-primary
            {:on-click #(rf/dispatch [:generate-prosets material-id @difficulty])}
            "Generate Practice"]
           [:button.btn.btn-secondary
            {:on-click #(do
                          (rf/dispatch [:open-bank-soal material-id])
                          (set! (.-hash js/location) "#bank-soal"))}
            "Bank Soal"]
           [:button.btn.btn-accent
            {:on-click #(do
                          (rf/dispatch [:fetch-all-questions material-id])
                          (set! (.-hash js/location) (str "#practice-all/" material-id)))}
            "Tes Semua Soal Materi Ini"]]
          [:p.small "Pilih Difficulty:"]
           [:select
           {:value @difficulty
            :on-change #(reset! difficulty (.. % -target -value))}
           [:option {:value "easy"} "Easy"]
           [:option {:value "medium"} "Medium"]
           [:option {:value "hard"} "Hard"]]]
         [:div.panel
          [:p.small "Material-id lom ada."]
          [:button.btn.btn-primary
           {:on-click #(rf/dispatch [:fetch-materials])}
           "Refresh daftar materi"]])])))

(defn bank-soal-page []
  (let [ps @(rf/subscribe [:prosets])]
    [:div.container
     [:h2.page-title "Bank Soal"]
     (if (seq ps)
       [:ul.material-list
        (for [p ps]
          ^{:key (:_id p)}
          [:li
           [:a {:href (str "#practice-proset/" (:_id p))
                :on-click (fn [e]
                            (.preventDefault e)
                            (rf/dispatch [:fetch-proset-by-id (:_id p)]))}
            [:span (or (:bank-code p) (str "Proset " (:_id p)))]
            " — "
            [:span.badge (or (:difficulty p) "")]]])]
       [:div.panel
        [:p "Belum ada bank soal untuk materi ini."]
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:generate-prosets (:_id @(rf/subscribe [:current-material])) "medium"])}
         "Generate Bank Soal"]])]))

(defn practice-proset-page []
  (let [p @(rf/subscribe [:current-proset])
        answers (r/atom {})]
    [:div.container
     [:h2 (str (or (:bank-code p) (:topic p)))]
     (if (seq (:problems p)) 
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems p))]
           ^{:key (str "proset-q-" idx)}
           [:li.question
            [:div.problem (:problem q)]
            [:ul.options
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "proset-opt-" idx "-" i)}
               [:li.option
                [:label
                 [:input {:type "radio"
                          :name (str "proset-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 [:span (str (char (+ 65 i)) ". " (or (:text choice) choice))]]])]])]
        [:div {:style {:margin-top "1em"}}
         [:button.btn.btn-success
          {:on-click #(rf/dispatch
                       [:submit-proset
                        (:_id p)
                        (mapv (fn [i] 
                                {:selected (get @answers i nil)})
                              (range (count (:problems p))))])}
          "Submit Jawaban"]]]

       [:div.panel
        [:p "Soal kosong bro, coba generate dulu."]])]))

(defn practice-all-page [material-id]
  (let [answers (r/atom {})
        all @(rf/subscribe [:all-questions])]
    [:div.container
     [:h2.page-title "Tes Semua Soal Materi Ini"]
     (if (seq (:problems all))
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems all))]
           ^{:key (str "all-q-" idx)}
           [:li.question
            [:div.problem (:problem q)]
            [:ul.options
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "all-opt-" idx "-" i)}
               [:li.option
                [:label
                 [:input {:type "radio"
                          :name (str "all-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 [:span {:style {:margin-left "0.6em"}}
                  (str (char (+ 65 i)) ". " (or (:text choice) choice))]]])]])]
        [:div {:style {:margin-top "1em"}}
         [:button.btn.btn-success
          {:on-click #(rf/dispatch
                       [:submit-all-questions
                        material-id
                        (mapv (fn [i]
                                {:selected (get @answers i nil)})
                              (range (count (:problems all))))])}
          "Submit Semua Jawaban"]]]

       [:p "Belum ada soal di materi ini."])]))

(defn practice-all-user-page []
  (let [answers (r/atom {})
        all @(rf/subscribe [:user-all-questions])]
    [:div.container
     [:h2.page-title "Tes Semua Soal dari Semua Materi"]
     (if (seq (:problems all)) 
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems all))]
           ^{:key (str "all-user-q-" idx)}
           [:li.question
            [:div.problem (:problem q)]
            [:ul.options
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "all-user-opt-" idx "-" i)}
               [:li.option
                [:label
                 [:input {:type "radio"
                          :name (str "all-user-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 [:span {:style {:margin-left "0.6em"}}
                  (str (char (+ 65 i)) ". " (or (:text choice) choice))]]])]])]
        [:div {:style {:margin-top "1em"}}
         [:button.btn.btn-accent
          {:on-click #(rf/dispatch
                       [:submit-user-all-questions
                        (mapv (fn [i]
                                {:selected (get @answers i nil)})
                              (range (count (:problems all))))])}
          "Submit Semua Jawaban"]]]

       [:p "Belum ada soal di semua materi lo."])]))

(defn practice-result-page []
  (let [result @(rf/subscribe [:last-proset-result])
        qs     (:problems result)]
    [:div.container
     [:h2.page-title "Hasil Practice"]
     (if (seq qs)
       [:div
        [:p.score
         (str "Skor: "
              (get-in result [:score :correct] 0)
              " / "
              (get-in result [:score :total] 0))]

        [:ol
         (for [[idx q] (map-indexed vector qs)]
           ^{:key (str "res-" idx)}
           [:li.question
            [:div.problem (:problem q)]
            [:ul.options
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "res-opt-" idx "-" i)}
               [:li.option
                (cond
                  (= i (:user-answer q))
                  [:span.user-answer {:class (if (:correct? q) "correct" "wrong")}
                   (str (char (+ 65 i)) ". " choice)
                   (if (:correct? q) " ✓" " ✗")]

                  (= i (:answer-idx q))
                  [:span.correct-answer
                   (str (char (+ 65 i)) ". " choice " (correct)")]

                  :else (str (char (+ 65 i)) ". " choice))])]

            (when-let [ex (get-in q [:explanation :en])]
              [:p.explanation
               [:em "Explanation: "] ex])

            (when-let [ex-id (get-in q [:explanation :id])]
              [:p.explanation
               [:em "Penjelasan: "] ex-id])])]]
       [:p "Belum ada hasil practice. Coba kerjain dulu tesnya!"])]))

;; -----------------------------------------------------------------------------
;; ROOT
;; -----------------------------------------------------------------------------
(defn app-root []
  (let [page @(rf/subscribe [:page])
        params @(rf/subscribe [:page-params])]
    [:div
     (case page
       :auth [auth-page]
       :home       [home-page]
       :materials  [materials-page]
       :material   [material-page]
       :generate   [generate-page]
       :practice   [practice-page (first params)]
       :bank-soal [bank-soal-page]
       :practice-proset [practice-proset-page]
       :custom-prosets [custom-proset-page]
       :practice-custom [custom-proset-test-page]
       :practice-result [practice-result-page]
       :practice-all [practice-all-page (first params)]
       :practice-all-user [practice-all-user-page]
       :assessment [assessment-page]
       :assessment-test [assessment-test-page]
       :assessment-result [assessment-result-page]
       [:div "Halaman tidak ditemukan."])]))

