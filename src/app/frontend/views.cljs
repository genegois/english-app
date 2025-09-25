(ns app.frontend.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

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
(defn login-page []
  (let [email (r/atom "")]
    (fn []
      [:div.container
       [:h2 "Login"]
       [input-field email "Email" "email"]
       [:div
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:login {:email @email}])}
         "Login"]]
       [:p "Belum punya akun? "
        [:a.link {:href "#register"} "Register"]]])))

(defn register-page []
  (let [email (r/atom "")
        username (r/atom "")]
    (fn []
      [:div.container
       [:h2 "Register"]
       [input-field email "Email" "email"]
       [input-field username "Username" "text"]
       [:div
        [:button.btn.btn-success
         {:on-click #(rf/dispatch [:register {:email @email
                                              :username @username}])}
         "Register"]]
       [:p "Udah punya akun? "
        [:a.link {:href "#login"} "Login"]]])))
;; -----------------------------------------------------------------------------
;; HOME
;; -----------------------------------------------------------------------------
(defn home-page []
  [:div.container
   [:h2 "Home"]
   [:div.btn-group
    [:button.btn {:on-click #(set! (.-hash js/location) "#generate")} "Generate manual"]
    [:button.btn {:on-click #(set! (.-hash js/location) "#materials")} "List materi"]
    [:button.btn {:on-click #(set! (.-hash js/location) "#assessment")} "Tes skill lo"]]])

;; -----------------------------------------------------------------------------
;; MATERIALS
;; -----------------------------------------------------------------------------
(defn materials-page []
  (let [materials @(rf/subscribe [:materials])]
    (fn []
      [:div.container
       [:h2.page-title "List Materi"]

       ;; tombol refresh
       [:div.row
        [:button.btn.btn-secondary
         {:on-click #(rf/dispatch [:fetch-materials])}
         "Refresh"]]

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
              (str (or (:topic m) "Untitled")
                   " — "
                   (or (:difficulty m) ""))]])]
         [:p.small "Belum ada materi."])

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
    (let [vals (vals items)]
      [:ul (for [[k v] items] ^{:key (str k)} [:li (str (name k) ": " (text-from v lang))])])
    :else [:div (text-from items lang)]))

(defn lang-block [title node]
  [:div
   [:p [:strong title]]
   [:ul
    [:li [:strong "EN: "] (text-from node :en)]
    [:li [:strong "ID: "] (text-from node :id)]]])

(defn material-page []
  (let [m @(rf/subscribe [:current-material])
        ps @(rf/subscribe [:prosets])]
    (fn []
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
              [:p.small "Practice belum ada / masih loading..."]
              [:button.btn.btn-success
               {:on-click #(rf/dispatch [:generate-prosets (:_id m) (:difficulty m)])}
               "Generate Practice"]])]]

         [:p.small "Materi belum dimuat. Klik dari List dulu."])])))


;; -----------------------------------------------------------------------------
;; GENERATE
;; -----------------------------------------------------------------------------
(defn generate-page []
  (let [topic (r/atom "")
        diff  (r/atom "easy")]
    (fn []
      [:div.container
       [:h2.page-title "Generate Materi"]

       ;; input topik
       [input-field topic "Topik (misal Vocabulary)" "text"]

       ;; select difficulty
       [:select.input-field
        {:value @diff
         :on-change #(reset! diff (.. % -target -value))}
        [:option {:value "easy"} "Easy"]
        [:option {:value "medium"} "Medium"]
        [:option {:value "hard"} "Hard"]]

       ;; tombol generate
       [:div
        [:button.btn.btn-success
         {:on-click #(rf/dispatch [:generate-material {:topic @topic :difficulty @diff}])}
         "Generate"]]

       ;; link balik ke list
       [:p.small
        [:a {:href "#materials"} "← Lihat List Materi"]]])))

;; -----------------------------------------------------------------------------
;; ASSESSMENT
;; -----------------------------------------------------------------------------
(defn assessment-page []
  [:div.container
   [:h2.page-title "Assessment"]
   [:p.desc
    "Assessment ini adalah tes menyeluruh untuk ngecek kemampuan lo "
    "dalam grammar, vocabulary, dan reading. Anggep aja ini ‘ultimate test’ "
    "buat tau level lo sekarang."]

   [:div.btn-group
    [:button.btn.btn-primary
     {:on-click #(rf/dispatch [:generate-assessment])}
     "Generate Assessment"]

    [:button.btn.btn-secondary
     {:on-click #(do
                   (rf/dispatch [:fetch-assessment])
                   (set! (.-hash js/location) "#assessment-test"))}
     "Mulai Tes!"]]])

(defn assessment-test-page []
  (let [a @(rf/subscribe [:assessment])
        answers (r/atom {})]
    (fn []
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
                         (mapv (fn [[i sel]] {:selected sel})
                               (sort-by key @answers))])}
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
                [:button.btn.btn-accent
                 {:on-click #(rf/dispatch [:generate-weak-topics])}
                 "Generate materi buat topik lemah"]
                [:p "Materi lemah sudah digenerate!"])]
             [:p "Ngeri kali brok! Nggak ada topik lemah nih 🎉"])])]

       [:p "Belum ada hasil assessment. Coba kerjain dulu gih tesnya!"])]))


;; -----------------------------------------------------------------------------
;; PRACTICE
;; -----------------------------------------------------------------------------
(defn practice-page [material-id]
  (let [ps @(rf/subscribe [:prosets])
        last @(rf/subscribe [:last-proset-result])]
    [:div.container
     [:h2.page-title "Practice Menu"]
     (if (some? material-id)
       [:div
        [:div.btn-group
         [:button.btn.btn-primary
          {:on-click #(rf/dispatch [:generate-prosets material-id "medium"])}
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
          "Tes Semua Soal Materi Ini"]]]
       [:div.panel
        [:p.small "Material-id belum tersedia. Masuk ke halaman materi lewat List Materi atau refresh."]
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:fetch-materials])}
         "Refresh daftar materi"]])]))

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
                        (mapv (fn [[i sel]] {:selected sel})
                              (sort-by key @answers))])}
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
                        (mapv (fn [[i sel]] {:selected sel})
                              (sort-by key @answers))])}
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
                        (mapv (fn [[i sel]] {:selected sel})
                              (sort-by key @answers))])}
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
       :register   [register-page]
       :login      [login-page]
       :home       [home-page]
       :materials  [materials-page]
       :material   [material-page]
       :generate   [generate-page]
       :practice   [practice-page (first params)]
       :bank-soal [bank-soal-page]
       :practice-proset [practice-proset-page]
       :practice-result [practice-result-page]
       :practice-all [practice-all-page (first params)]
       :practice-all-user [practice-all-user-page]
       :assessment [assessment-page]
       :assessment-test [assessment-test-page]
       :assessment-result [assessment-result-page]
       [:div "Halaman tidak ditemukan."])]))



