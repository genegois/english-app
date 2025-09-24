(ns app.frontend.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; -----------------------------------------------------------------------------
;; Helper Input
;; -----------------------------------------------------------------------------
(defn input-field [val-atom placeholder type]
  [:input {:type type
           :value (or @val-atom "")
           :placeholder placeholder
           :style {:display "block"
                   :margin "0.5em 0"
                   :padding "0.5em"
                   :width "250px"}
           :on-change #(reset! val-atom (.. % -target -value))}])

;; -----------------------------------------------------------------------------
;; AUTH
;; -----------------------------------------------------------------------------
(defn login-page []
  (let [email (r/atom "")]
    (fn []
      [:div
       [:h2 "Login"]
       [input-field email "Email" "email"]
       [:div
        [:button {:on-click #(rf/dispatch [:login {:email @email}])}
         "Login"]]
       [:p "Belum punya akun? "
        [:a {:href "#register"} "Register"]]])))

(defn register-page []
  (let [email (r/atom "")
        username (r/atom "")]
    (fn []
      [:div
       [:h2 "Register"]
       [input-field email "Email" "email"]
       [input-field username "Username" "text"]
       [:div
        [:button {:on-click #(rf/dispatch [:register {:email @email
                                                      :username @username}])}
         "Register"]]
       [:p "Udah punya akun? "
        [:a {:href "#login"} "Login"]]])))
;; -----------------------------------------------------------------------------
;; HOME
;; -----------------------------------------------------------------------------
(defn home-page []
  [:div
   [:h2 "Home"]
   [:div
    [:button {:on-click #(set! (.-hash js/location) "#generate")} "Generate manual"]
    [:button {:on-click #(set! (.-hash js/location) "#materials")} "List materi"]
    [:button {:on-click #(set! (.-hash js/location) "#assessment")} "Tes skill lo"]]])

;; -----------------------------------------------------------------------------
;; MATERIALS
;; -----------------------------------------------------------------------------
(defn materials-page []
  (let [materials @(rf/subscribe [:materials])]
    (fn []
      [:div
       [:h2 "List Materi"]
       [:button {:on-click #(rf/dispatch [:fetch-materials])} "Refresh"]
       (if (seq materials)
         [:ul
          (for [m materials]
            ^{:key (str (:_id m))}
            [:li
             [:a {:href (str "#material/" (:_id m))
                  :on-click (fn [e]
                              (.preventDefault e)
                              (rf/dispatch [:fetch-material (:_id m)]))}
              (str (or (:topic m) "Untitled")
                   " — "
                   (or (:difficulty m) ""))]])]
         [:p "Belum ada materi."])
       [:div {:style {:margin-top "2em"}}
        [:h3 "Latihan Gabungan Semua Materi"]
        [:button
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
      [:div
       [:h2 "Detail Materi"]
       (if m
         [:div
          [:p [:strong "Topic: "] (or (:topic m) "—")]

          (lang-block "Definition" (get-in m [:content :definition]))
          (lang-block "Usage" (get-in m [:content :usage]))
          (lang-block "Importance" (get-in m [:content :importance]))
          (lang-block "Tips" (get-in m [:content :tips]))

          ;; common mistakes
          [:div
           [:p [:strong "Common mistakes"]]
           (render-multi (get-in m [:content :common-mistakes :en]) :en)
           [:p [:strong "Kesalahan umum"]]
           (render-multi (get-in m [:content :common-mistakes :id]) :id)]

          ;; examples
          [:div
           [:p [:strong "Examples"]]
           (let [ex (get-in m [:content :examples])]
             [:ul
              [:li (str "Positive EN: " (text-from (get-in ex [:positive]) :en))]
              [:li (str "Positive ID: " (text-from (get-in ex [:positive]) :id))]
              [:li (str "Negative EN: " (text-from (get-in ex [:negative]) :en))]
              [:li (str "Negative ID: " (text-from (get-in ex [:negative]) :id))]])]

          ;; practice
          [:div
           [:h3 "Practice"]
           (if (seq ps)
             [:div
              [:button {:on-click #(set! (.-hash js/location) (str "#practice/" (:_id m)))} "Mulai Practice"]]
             [:div
              [:p "Practice belum ada / masih loading..."]
              [:button {:on-click #(rf/dispatch [:generate-prosets (:_id m) (:difficulty m)])}
               "Generate Practice"]])]]

         [:p "Materi belum dimuat. Klik dari List dulu."])])))

;; -----------------------------------------------------------------------------
;; GENERATE
;; -----------------------------------------------------------------------------
(defn generate-page []
  (let [topic (r/atom "")
        diff  (r/atom "easy")]
    (fn []
      [:div
       [:h2 "Generate Materi"]
       [input-field topic "Topik (misal Vocabulary)" "text"]
       [:select {:value @diff
                 :on-change #(reset! diff (.. % -target -value))}
        [:option {:value "easy"} "Easy"]
        [:option {:value "medium"} "Medium"]
        [:option {:value "hard"} "Hard"]]
       [:div
        [:button {:on-click #(rf/dispatch [:generate-material {:topic @topic :difficulty @diff}])}
         "Generate"]]
       [:p [:a {:href "#materials"} "Lihat List Materi"]]])))

;; -----------------------------------------------------------------------------
;; ASSESSMENT
;; -----------------------------------------------------------------------------
(defn assessment-page []
  [:div
   [:h2 "Assessment"]
   [:button {:on-click #(rf/dispatch [:generate-assessment])} "Generate Assessment"]
   [:button {:on-click #(do
                          (rf/dispatch [:fetch-assessment])
                          (set! (.-hash js/location) "#assessment-test"))}
    "Mulai Tes"]])

(defn assessment-test-page []
  (let [a @(rf/subscribe [:assessment])
        answers (r/atom {})]
    [:div
     [:h2 "Assessment Test"]
     (if (seq (:questions a))
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:questions a))]
           ^{:key (str "assess-q-" idx)}
           [:li
            [:div (:problem q)]
            [:ul
             (for [[i opt] (map-indexed vector (:options q))]
               ^{:key (str "assess-opt-" idx "-" i)}
               [:li
                [:label
                 [:input {:type "radio"
                          :name (str "assess-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 (str (char (+ 65 i)) ". " opt)]])]])]
        [:button
         {:on-click #(rf/dispatch
                      [:submit-assessment
                       (mapv (fn [[i sel]] {:selected sel})
                             (sort-by key @answers))])}
         "Submit Jawaban"]]
       [:p "Belum ada assessment gan, generate dulu!"])]))


(defn assessment-result-page []
  (let [result @(rf/subscribe [:assessment])]
    [:div
     [:h2 "Hasil Assessment"]
     (if result
       [:div
        ;; skor
        [:p [:strong "Skor: "]
         (str (get-in result [:score :correct] 0)
              " / "
              (get-in result [:score :total] 0))]

        ;; soal-soal
        [:ol
         (for [[idx q] (map-indexed vector (:questions result))]
           ^{:key (str "ass-res-" idx)}
           [:li
            [:div (:problem q)]
            [:ul
             (for [[i choice] (map-indexed vector (:options q))]
               ^{:key (str "ass-opt-" idx "-" i)}
               [:li
                (cond
                  (= i (:user-answer q))
                  [:span {:style {:font-weight "bold"
                                  :color (if (:correct? q) "green" "red")}}
                   (str (char (+ 65 i)) ". " choice)
                   (if (:correct? q) " ✓" " ✗")]

                  (= i (:answer-idx q))
                  [:span {:style {:color "green"}}
                   (str (char (+ 65 i)) ". " choice " (correct)")]

                  :else (str (char (+ 65 i)) ". " choice))])]
            [:p [:em "Explanation: "] (get-in q [:explanation :en])]
            [:p [:em "Penjelasan: "] (get-in q [:explanation :id])]])]

        ;; weak topics section
        (let [weak (or (:weak-topics result) [])
              already? (:weak-generated? result)]
          [:div
           [:h3 "Topik Lemah"]
           (if (seq weak)
             [:div
              [:ul (for [t weak] ^{:key t} [:li t])]
              (if-not already?
                [:button {:on-click #(rf/dispatch [:generate-weak-topics])}
                 "Generate materi buat topik lemah"]
                [:p "Materi lemah sudah digenerate!"])]
             [:p "Ngeri kali brok! Nggak ada topik lemah nih yang ketemu"])])]
       [:p "Belum ada hasil assessment. Coba kerjain dulu gih tesnya!"])]))

;; -----------------------------------------------------------------------------
;; PRACTICE
;; -----------------------------------------------------------------------------
(defn practice-page [material-id]
  (let [ps @(rf/subscribe [:prosets])
        last @(rf/subscribe [:last-proset-result])]
    [:div
     [:h2 "Practice Menu"]

     ;; safe generate: only dispatch when we have material-id
     (if (some? material-id)
       [:div
        [:button {:on-click #(rf/dispatch [:generate-prosets material-id "medium"])}
         "Generate Practice"]
        [:button
         {:on-click #(do
                       (rf/dispatch [:open-bank-soal material-id])
                       (set! (.-hash js/location) "#bank-soal"))}
         "Bank Soal"]

        [:button {:on-click #(do
                               (rf/dispatch [:fetch-all-questions material-id])
                               (set! (.-hash js/location) (str "#practice-all/" material-id)))}
         "Tes Semua Soal Materi Ini"]

        (when (:user @(rf/subscribe [:user]))
          [:button {:on-click #(do
                                 (rf/dispatch [:fetch-last-proset])
                                 (set! (.-hash js/location) "#practice-test"))}
           "Berlatih soal generate terakhir!"])]
       [:div
        [:p "Material-id belum tersedia. Pastikan lo masuk ke halaman materi lewat List Materi atau refresh halaman untuk load materi."]
        [:button {:on-click #(rf/dispatch [:fetch-materials])} "Refresh daftar materi"]])]))

(defn bank-soal-page []
  (let [ps @(rf/subscribe [:prosets])]
    [:div
     [:h2 "Bank Soal"]
     (if (seq ps)
       [:ul
        (for [p ps]
          ^{:key (:_id p)}
          [:li
           [:a {:href (str "#practice-proset/" (:_id p))
                :on-click (fn [e]
                            (.preventDefault e)
                            (rf/dispatch [:fetch-proset-by-id (:_id p)]))}
            (or (:bank-code p) (str "Proset " (:_id p)))
            " — "
            (or (:difficulty p) "")]])]
       [:p "Belum ada bank soal untuk materi ini."])]))


(defn practice-proset-page []
  (let [p @(rf/subscribe [:current-proset])
        answers (r/atom {})]
    [:div
     [:h2 (str "Bank Soal - " (or (:bank-code p) (:topic p)))]
     (if (seq (:problems p))
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems p))]
           ^{:key (str "proset-q-" idx)}
           [:li
            [:div (:problem q)]
            [:ul
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "proset-opt-" idx "-" i)}
               [:li
                [:label
                 [:input {:type "radio"
                          :name (str "proset-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 (str (char (+ 65 i)) ". " (or (:text choice) choice))]])]])]
        [:button
         {:on-click #(rf/dispatch
                      [:submit-proset
                       (:_id p)
                       (mapv (fn [[i sel]] {:selected sel})
                             (sort-by key @answers))])}
         "Submit Jawaban"]]
       [:p "Soal kosong bro, coba generate dulu."])]))




(defn practice-test-page []
  (let [ps @(rf/subscribe [:prosets])
        answers (r/atom {})]
    [:div
     [:h2 "Practice Test"]
     (if (seq ps)
       (let [problems (:problems (first ps))
             proset-id (:_id (first ps))]
         [:div
          [:ol
           (for [[idx p] (map-indexed vector problems)]
             ^{:key (str "p-" idx)}
             [:li
              [:div (:problem p)]
              [:ul
               (for [[i c] (map-indexed vector (:choices p))]
                 ^{:key (str "p-opt-" idx "-" i)}
                 [:li
                  [:label
                   [:input {:type "radio"
                            :name (str "p-q" idx) ;; grup per soal
                            :value i
                            ;; jangan pakek :checked
                            :on-change #(swap! answers assoc idx i)}]
                   (str (char (+ 65 i)) ". " (or (:text c) c))]])]])]
          [:button
           {:on-click #(rf/dispatch
                        [:submit-proset
                         proset-id
                         (mapv (fn [[i sel]] {:selected sel})
                               (sort-by key @answers))])}
           "Submit Jawaban"]])
       [:p "Belum ada practice, generate dulu di menu Practice!"])]))

(defn practice-result-page []
  (let [result @(rf/subscribe [:last-proset-result])]
    [:div
     [:h2 "Hasil Practice"]
     (if result
       [:div
        [:p [:strong "Skor: "]
         (str (get-in result [:score :correct] 0)
              " / "
              (get-in result [:score :total] 0))]
        [:ol
         (for [[idx q] (map-indexed vector (:problems result))]
           ^{:key (str "res-" idx)}
           [:li
            [:div (:problem q)]
            [:ul
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "res-opt-" idx "-" i)}
               [:li
                (cond
                  (= i (:user-answer q))
                  [:span {:style {:font-weight "bold"
                                  :color (if (:correct? q) "green" "red")}}
                   (str (char (+ 65 i)) ". " choice)
                   (if (:correct? q) " ✓" " ✗")]

                  (= i (:answer-idx q))
                  [:span {:style {:color "green"}}
                   (str (char (+ 65 i)) ". " choice " (correct)")]

                  :else (str (char (+ 65 i)) ". " choice))])]
            [:p [:em "Explanation: "] (get-in q [:explanation :en])]
            [:p [:em "Penjelasan: "] (get-in q [:explanation :id])]])]]

       [:p "Belum ada hasil practice. Coba kerjain dulu tesnya!"])]))

(defn practice-all-page [material-id]
  (let [answers (r/atom {})
        all @(rf/subscribe [:all-questions])]
    [:div
     [:h2 "Tes Semua Soal Materi Ini"]
     (if (seq (:problems all))
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems all))]
           ^{:key (str "all-q-" idx)}
           [:li
            [:div (:problem q)]
            [:ul
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "all-opt-" idx "-" i)}
               [:li
                [:label
                 [:input {:type "radio"
                          :name (str "all-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 (str (char (+ 65 i)) ". " (or (:text choice) choice))]])]])]
        [:button
         {:on-click #(rf/dispatch
                      [:submit-all-questions
                       material-id
                       (mapv (fn [[i sel]] {:selected sel})
                             (sort-by key @answers))])}
         "Submit Semua Jawaban"]]
       [:p "Belum ada soal di materi ini."])]))

(defn practice-all-user-page []
  (let [answers (r/atom {})
        all @(rf/subscribe [:user-all-questions])]
    [:div
     [:h2 "Tes Semua Soal dari Semua Materi"]
     (if (seq (:problems all))
       [:div
        [:ol
         (for [[idx q] (map-indexed vector (:problems all))]
           ^{:key (str "all-user-q-" idx)}
           [:li
            [:div (:problem q)]
            [:ul
             (for [[i choice] (map-indexed vector (:choices q))]
               ^{:key (str "all-user-opt-" idx "-" i)}
               [:li
                [:label
                 [:input {:type "radio"
                          :name (str "all-user-q" idx)
                          :value i
                          :on-change #(swap! answers assoc idx i)}]
                 (str (char (+ 65 i)) ". " (or (:text choice) choice))]])]])]
        [:button
         {:on-click #(rf/dispatch
                      [:submit-user-all-questions
                       (mapv (fn [[i sel]] {:selected sel})
                             (sort-by key @answers))])}
         "Submit Semua Jawaban"]]
       [:p "Belum ada soal di semua materi lo."])]))



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
       :practice-test [practice-test-page]
       :practice-result [practice-result-page]
       :practice-all [practice-all-page (first params)]
       :practice-all-user [practice-all-user-page]
       :assessment [assessment-page]
       :assessment-test [assessment-test-page]
       :assessment-result [assessment-result-page]
       [:div "Halaman tidak ditemukan."])]))



