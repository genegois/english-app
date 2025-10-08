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
  (let [login-email  (r/atom "")
        reg-email    (r/atom "")
        reg-username (r/atom "")]
    (fn []
      [:div.container
       {:style {:max-width "500px"
                :margin "2em auto"
                :text-align "center"}}

       ;; Judul & tagline
       [:h1 {:style {:font-weight "700"
                     :font-size "1.8em"
                     :margin-bottom "0.3em"}}
        "🎓 English Learning nih"]
       [:p {:style {:font-size "0.95em"
                    :color "#666"
                    :margin-bottom "2em"}}
        "Coba ae dulu"]

       ;; --- REGISTER ---
       [:div.panel
        {:style {:padding "1.5em"
                 :margin-bottom "1.5em"
                 :border "1px solid #ddd"
                 :border-radius "10px"
                 :background "#fafafa"
                 :box-shadow "0 2px 4px rgba(0,0,0,0.05)"}}
        [:h2 {:style {:margin-bottom "1em"
                      :color "#333"
                      :font-size "1.3em"}}
         "📝 Register Baru"]

        [input-field reg-email "Email" "email"]
        [input-field reg-username "Username" "text"]

        [:button.btn.btn-success
         {:style {:width "100%"
                  :margin-top "1em"}
          :on-click #(rf/dispatch
                      [:register {:email @reg-email
                                  :username @reg-username}])}
         "Daftar Sekarang"]]

       ;; --- PEMISAH ---
       [:div {:style {:margin "1.5em 0"
                      :color "#aaa"
                      :font-size "0.9em"
                      :display "flex"
                      :align-items "center"
                      :justify-content "center"}}
        [:hr {:style {:flex "1"
                      :border "none"
                      :border-top "1px solid #ddd"
                      :margin "0 10px"}}]
        "atau"
        [:hr {:style {:flex "1"
                      :border "none"
                      :border-top "1px solid #ddd"
                      :margin "0 10px"}}]]

       ;; --- LOGIN ---
       [:div.panel
        {:style {:padding "1.5em"
                 :border "1px solid #ddd"
                 :border-radius "10px"
                 :background "#fefefe"
                 :box-shadow "0 2px 4px rgba(0,0,0,0.05)"}}
        [:h2 {:style {:margin-bottom "1em"
                      :color "#333"
                      :font-size "1.3em"}}
         "🔐 Login"]

        [input-field login-email "Email" "email"]

        [:button.btn.btn-primary
         {:style {:width "100%"
                  :margin-top "1em"}
          :on-click #(rf/dispatch [:login {:email @login-email}])}
         "Masuk"]]

       ;; Footer kecil
       [:p {:style {:margin-top "2em"
                    :font-size "0.8em"
                    :color "#aaa"}}
        "© 2025 PT. Zona Pengembangan Xpertise"]])))

;; -----------------------------------------------------------------------------
;; HOME
;; -----------------------------------------------------------------------------
(defn home-page []
  [:div.container
   {:style {:max-width "600px"
            :margin "2em auto"
            :text-align "center"}}

   ;; judul utama
   [:h1 {:style {:font-weight "700"
                 :font-size "2em"
                 :margin-bottom "0.3em"}}
    "🎯 English Learning Hub"]

   [:p {:style {:font-size "1em"
                :color "#666"
                :margin-bottom "2em"}}
    "Tempat lo berlatih Bahasa Inggris, generate soal, dan nguji kemampuan lo. 💪"]

   ;; menu utama
   [:div {:style {:display "flex"
                  :flex-direction "column"
                  :gap "1em"}}

    [:button.btn.btn-primary
     {:style {:padding "1em"
              :font-size "1.1em"
              :border-radius "8px"
              :cursor "pointer"}
      :on-click #(set! (.-hash js/location) "#generate")}
     "🧠 Generate Manual"]

    [:button.btn.btn-success
     {:style {:padding "1em"
              :font-size "1.1em"
              :border-radius "8px"
              :cursor "pointer"}
      :on-click #(set! (.-hash js/location) "#materials")}
     "📚 List Materi"]

    [:button.btn.btn-accent
     {:style {:padding "1em"
              :font-size "1.1em"
              :border-radius "8px"
              :cursor "pointer"}
      :on-click #(set! (.-hash js/location) "#assessment")}
     "🧩 Assessment"]]

   ;; footer kecil
   [:p {:style {:margin-top "2em"
                :font-size "0.85em"
                :color "#999"}}
    "Selamat belajar dan jangan lupa istirahat 😎"]])

;; -----------------------------------------------------------------------------
;; MATERIALS
;; -----------------------------------------------------------------------------
(defn materials-page []
  (fn []
    (let [materials @(rf/subscribe [:materials])]
      [:div.container
       {:style {:max-width "750px"
                :margin "3em auto"
                :font-family "Inter, sans-serif"}}

       ;; Title section
       [:h2 {:style {:font-size "2em"
                     :font-weight "700"
                     :margin-bottom "0.3em"
                     :text-align "center"}}
        "📚 List Materi"]

       [:p {:style {:text-align "center"
                    :color "#666"
                    :margin-bottom "1.8em"}}
        "Lihat semua materi yang udah lo generate. Bisa lo buka, latihanin, atau bikin gabungannya. 🔥"]

       ;; Toolbar buttons
       [:div {:style {:display "flex"
                      :justify-content "center"
                      :gap "1em"
                      :margin-bottom "2em"}}
        [:button.btn.btn-outline-primary
         {:style {:padding "0.6em 1.2em"
                  :font-size "1em"}
          :on-click #(rf/dispatch [:fetch-materials])}
         "🔄 Refresh"]
        [:button.btn.btn-secondary
         {:style {:padding "0.6em 1.2em"
                  :font-size "1em"}
          :on-click #(set! (.-hash js/location) "#/")}
         "🏠 Balik ke Home"]]

       ;; List of materials
       (if (seq materials)
         [:div
          (for [m materials]
            ^{:key (str (:_id m))}
            [:div.material-card
             {:style {:background "#fff"
                      :border "1px solid #ddd"
                      :border-radius "8px"
                      :padding "1em 1.2em"
                      :margin-bottom "1em"
                      :box-shadow "0 1px 3px rgba(0,0,0,0.05)"
                      :transition "transform 0.2s ease"
                      :cursor "pointer"}
              :on-click #(do
                           (rf/dispatch [:fetch-material (:_id m)])
                           (set! (.-hash js/location)
                                 (str "#material/" (:_id m))))}
             [:h4 {:style {:margin "0 0 0.3em 0"
                           :font-weight "600"
                           :color "#333"}}
              (or (:topic m) "Untitled")]
             [:p {:style {:margin 0
                          :font-size "0.9em"
                          :color "#777"}}
              "Klik buat buka materi ini"]])]
         [:p {:style {:text-align "center"
                      :color "#888"
                      :margin "2em 0"}}
          "Belum ada materi yang lo generate 😢"])

       ;; Custom proset
       [:div {:style {:margin-top "3em"
                      :text-align "center"}}
        [:button.btn.btn-accent
         {:style {:padding "0.8em 1.5em"
                  :font-size "1em"
                  :font-weight "500"
                  :border-radius "6px"
                  :background "#28a745"
                  :color "#fff"
                  :cursor "pointer"}
          :on-click #(set! (.-hash js/location) "#custom-proset")}
         "🧩 Buat latihan gabungan dari list materi lo"]]

       ;; All materials combined test
       [:div.panel {:style {:margin-top "3em"
                            :padding "1.5em"
                            :border-radius "8px"
                            :background "#f8f9fa"
                            :border "1px solid #eee"
                            :text-align "center"}}
        [:h3 {:style {:margin-bottom "0.7em"
                      :color "#333"
                      :font-weight "600"}}
         "🎯 Tes Semua Soal dari Semua Materi"]
        [:button.btn.btn-primary
         {:style {:padding "0.8em 1.6em"
                  :font-size "1em"
                  :border-radius "6px"}
          :on-click #(do
                       (rf/dispatch [:fetch-user-all-questions])
                       (set! (.-hash js/location) "#practice-all-user"))}
         "Mulai Tes 🔥"]]])))

(defn text-from [node lang]
  (cond
    (nil? node) nil
    (string? node) node
    (map? node)
    (or (get node lang)
        (get node :en)
        (get node "en")
        (get node :id)
        (get node "id")
        (pr-str node))
    :else (pr-str node)))

(defn lang-block [title node]
  [:div {:style {:margin-bottom "1.2em"}}
   [:h3 {:style {:color "#333"
                 :font-size "1.1em"
                 :font-weight "600"
                 :margin-bottom "0.5em"}}
    title]
   [:div {:style {:background "#f9f9f9"
                  :padding "0.8em 1em"
                  :border-radius "6px"
                  :border "1px solid #eee"}}
    [:p {:style {:margin-bottom "0.4em"}}
     [:strong "EN: "] (text-from node :en)]
    [:p {:style {:margin 0}}
     [:strong "ID: "] (text-from node :id)]]])

(defn render-multi [items lang]
  (when (seq items)
    [:ul {:style {:margin "0.5em 0 0.5em 1.5em"
                  :padding 0
                  :color "#333"}}
     (for [it items]
       ^{:key (text-from it lang)}
       [:li {:style {:margin-bottom "0.3em"}} (text-from it lang)])]))

(defn material-page []
  (fn []
    (let [m @(rf/subscribe [:current-material])
          ps @(rf/subscribe [:prosets])]
      [:div.container
       {:style {:max-width "800px"
                :margin "2em auto"
                :font-family "Inter, sans-serif"}}

       [:h2.page-title {:style {:margin-bottom "0.5em"}} "📘 Detail Materi"]
       (if m
         [:div
          [:h2 {:style {:font-size "1.6em"
                        :margin-bottom "0.8em"
                        :color "#222"}}
           (or (:topic m) "Untitled")]

          [:div {:style {:margin-bottom "1em"
                         :color "#777"}}
           "Materi ini mencakup definisi, penggunaan, dan contoh dalam bahasa Inggris dan Indonesia."]

          ;; Main content
          [:div.panel {:style {:margin-bottom "1.5em"}}
           (lang-block "Definition" (get-in m [:content :definition]))
           (lang-block "Usage" (get-in m [:content :usage]))
           (lang-block "Importance" (get-in m [:content :importance]))
           (lang-block "Tips" (get-in m [:content :tips]))]

          ;; Common mistakes
          [:div.panel {:style {:margin-bottom "1.5em"}}
           [:h3.subtle "❌ Common Mistakes (EN)"]
           (render-multi (get-in m [:content :common-mistakes :en]) :en)
           [:h3.subtle "⚠️ Kesalahan Umum (ID)"]
           (render-multi (get-in m [:content :common-mistakes :id]) :id)]

          ;; Examples
          (let [ex (get-in m [:content :examples])]
            [:div.panel
             [:h3.subtle "🧩 Examples"]
             [:ul {:style {:margin-left "1.5em"}}
              [:li (str "Positive (EN): " (text-from (get-in ex [:positive]) :en))]
              [:li (str "Positive (ID): " (text-from (get-in ex [:positive]) :id))]
              [:li (str "Negative (EN): " (text-from (get-in ex [:negative]) :en))]
              [:li (str "Negative (ID): " (text-from (get-in ex [:negative]) :id))]]])

          ;; Practice section
          [:div.panel {:style {:margin-top "2em"
                               :text-align "center"
                               :padding "1em"}}
           (if (seq ps)
             [:button.btn.btn-primary
              {:style {:padding "0.8em 1.4em"
                       :font-size "1em"}
               :on-click #(set! (.-hash js/location)
                                (str "#practice/" (:_id m)))}
              "🎯 Buka Practice"]
             [:p.small "Practice belum ada boss. Generate dulu."])]]
         [:p.small "Materi belum dimuat. Klik dari List dulu."])])))

;; -----------------------------------------------------------------------------
;; GENERATE
;; -----------------------------------------------------------------------------
(defn generate-page []
  (let [topic (r/atom "")]
    (fn []
      [:div.container
       {:style {:max-width "600px"
                :margin "3em auto"
                :text-align "center"}}

       ;; judul
       [:h2 {:style {:font-size "2em"
                     :font-weight "700"
                     :margin-bottom "0.5em"}}
        "⚡ Generate Materi Baru"]

       [:p {:style {:font-size "1em"
                    :color "#555"
                    :margin-bottom "1.5em"}}
        "Masukin topik yang mau lo pelajarin — bisa satu kata (‘Preposition’) atau bacot bebas sesuka lo"]

       ;; input field lebih lebar & empuk
       [:input
        {:type "text"
         :placeholder "Topik / Konteks Belajar lo"
         :value @topic
         :on-change #(reset! topic (.. % -target -value))
         :style {:width "100%"
                 :padding "0.3em 0.5em"
                 :font-size "1.09em"
                 :border "1px solid #ccc"
                 :border-radius "6px"
                 :box-sizing "border-box"
                 :margin-bottom "1.5em"}}]

       ;; tombol diseragamkan proporsinya
       [:div
        [:button.btn.btn-success
         {:style {:padding "0.6em 0.6em"
                  :font-size "1em"
                  :border-radius "6px"
                  :cursor "pointer"}
          :on-click #(rf/dispatch [:generate-material {:topic @topic}])}
         "Generate Materi!"]]

       ;; link balik
       [:p {:style {:margin-top "2em"
                    :font-size "0.9em"}}
        [:a {:href "#materials"
             :style {:color "#007bff"
                     :text-decoration "none"}}
         "Lihat list materi lo"]]])))

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
     [:h2.page-title "🧩 Assessment"]

     (cond
       ;; ✅ Case 1: Baru aja ngerjain (cooldown sejam)
       (and submitted? last-ts (< (- now last-ts) lockout-ms))
       [:div.panel {:style {:text-align "center"
                            :background "#fdf6ec"
                            :border "1px solid #f5d2a0"
                            :border-radius "8px"
                            :padding "1.5em"}}
        [:h3 {:style {:color "#a65b00"}} "⏳ Tunggu Dulu, Bro!"]
        [:p {:style {:color "#704214" :margin-bottom "1em"}}
         "Weitsss, lo udah ngerjain assessment barusan. Tunggu sejam lagi yakk."]
        [:button.btn.btn-primary
         {:on-click #(set! (.-hash js/location) "#assessment-result")}
         "Lihat hasil terakhir lo"]]

       ;; 🧭 Case 2: Assessment udah digenerate tapi belum mulai
       (and assessment (not submitted?))
       [:div.panel {:style {:text-align "center"
                            :background "#eef6ff"
                            :border "1px solid #b8daff"
                            :border-radius "8px"
                            :padding "2em"}}
        [:h3 {:style {:margin-bottom "0.5em"}} "🚀 Ready to Begin?"]
        [:p {:style {:color "#555" :margin-bottom "1.25em"}}
         "Assessment ini bakal nguji semua aspek — grammar, vocab, dan reading comprehension."]
        [:button.btn.btn-primary
         {:on-click #(do
                       (rf/dispatch [:fetch-assessment])
                       (set! (.-hash js/location) "#assessment-test"))}
         "Mulai Tes Sekarang!"]]

       ;; 🧱 Case 3: Belum ada assessment → generate baru
       :else
       [:div.panel {:style {:text-align "center"
                            :background "#fafafa"
                            :border "1px solid #ddd"
                            :border-radius "8px"
                            :padding "2em"
                            :box-shadow "0 1px 3px rgba(0,0,0,0.1)"}}
        [:h3 "🎯 Siap Uji Kemampuan Lo?"]
        [:p {:style {:color "#444" :max-width "500px" :margin "0 auto 1.5em"}}
         "Assessment ini kayak placement test — ngebantu lo ngerti sejauh apa progress lo
          di grammar, vocabulary, sama reading comprehension. Gak usah takut, ini cuma latihan."]
        [:button.btn.btn-success
         {:on-click #(rf/dispatch [:generate-assessment])}
         "Generate Assessment"]])]))

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
  (let [selected (r/atom #{})
        title (r/atom "")]
    (fn []
      (let [materials @(rf/subscribe [:materials])
            custom @(rf/subscribe [:custom-proset])
            user @(rf/subscribe [:user])
            custom-list @(rf/subscribe [:user-custom-prosets])
            loading? @(rf/subscribe [:loading-custom-prosets?])]
        [:div.container
         [:h2.page-title {:style {:margin-bottom "0.5em"
                                  :font-weight "700"
                                  :font-size "1.6em"
                                  :color "#222"}}
          "🧩 Buat Latihan Gabungan Custom"]

         ;; --- Form Card ---
         [:div.panel {:style {:background "#ffffff"
                              :border "1px solid #e0e0e0"
                              :border-radius "10px"
                              :padding "1.5em"
                              :box-shadow "0 2px 8px rgba(0,0,0,0.05)"
                              :margin-bottom "2em"}}

          [:h3 {:style {:margin-top 0
                        :margin-bottom "1em"
                        :font-weight "600"
                        :font-size "1.2em"
                        :color "#333"}}
           "📘 Pilih Materi"]

          ;; Input judul
          [input-field title "Judul (opsional) — misal: Gabungan Love + Nurture" "text"]

          ;; Checkbox daftar materi
[:div {:style {:margin-top "1em"
               :display "grid"
               :grid-template-columns "1fr 1fr"
               :gap "0.5em"}}
 (doall
  (for [m materials]
    ^{:key (:_id m)}
    [:div {:style {:display "flex"
                   :align-items "center"
                   :gap "0.5em"
                   :background (if (contains? @selected (:_id m)) "#eef6ff" "#fafafa")
                   :border "1px solid #ddd"
                   :padding "0.5em 0.75em"
                   :border-radius "6px"
                   :cursor "pointer"
                   :transition "all 0.2s ease"}
           :on-click #(swap! selected
                             (fn [s]
                               (if (contains? s (:_id m))
                                 (disj s (:_id m))
                                 (conj s (:_id m)))))}
     [:input {:type "checkbox"
              :checked (contains? @selected (:_id m))
              :readOnly true ;; biar React gak warning
              :style {:pointer-events "none"}}]
     [:span {:style {:font-size "0.95em"}}
      (or (get-in m [:content :topic])
          (:topic m)
          "Untitled")]]))]

          ;; Tombol generate
          [:div {:style {:margin-top "1.5em"
                         :text-align "center"}}
           [:button.btn.btn-primary
            {:style {:font-weight "600"
                     :padding "0.8em 1.5em"
                     :font-size "1em"}
             :on-click #(rf/dispatch [:generate-custom-proset (vec @selected) @title])}
            "✨ Generate Gabungan"]]]

         ;; --- Result Section ---
         (when custom
           [:div {:style {:margin-top "1.5em"
                          :padding "1em"
                          :background "#f9f9f9"
                          :border "1px solid #ddd"
                          :border-radius "8px"}}
            [:h3 "✅ Latihan berhasil dibuat!"]
            [:p (str "Judul: " (:topic custom))]
            [:p (str "Jumlah Soal: " (count (:problems custom)))]
            [:div {:style {:margin-top "0.5em"
                           :display "flex"
                           :gap "0.5em"}}
             [:button.btn.btn-sm.btn-success
              {:on-click #(do
                            (rf/dispatch [:fetch-custom-proset-by-id (:_id custom)])
                            (set! (.-hash js/location)
                                  (str "#practice-custom/" (:_id custom))))}
              "Gasss!!"]
             [:button.btn.btn-sm.btn-danger
              {:on-click #(when (js/confirm "Lo yakin mau hapus latihan ini?")
                            (rf/dispatch [:delete-custom-proset
                                          (:_id custom)
                                          (or (:id user) (:_id user) (:email user))]))}
              "🗑️ Hapus"]]])

         [:hr {:style {:margin "2.5em 0"}}]

         ;; --- List Custom Prosets ---
         [:h3 {:style {:margin-bottom "0.5em"}} "🧠 Latihan Gabungan Lo"]
         [:div {:style {:margin-bottom "1em"}}
          [:button.btn.btn-outline-primary
           {:on-click #(rf/dispatch
                        [:fetch-user-custom-prosets
                         (or (:id user) (:_id user) (:email user))])}
           (if loading? "Loading..." "🔄 Refresh List")]]


         (cond
           loading?
           [:p "Sedang memuat..."]

           (seq custom-list)
           [:div.custom-list
            (doall
             (for [c (reverse custom-list)]
               ^{:key (:_id c)}
               [:div.custom-item
                {:style {:border "1px solid #e0e0e0"
                         :padding "1em"
                         :margin-bottom "0.8em"
                         :border-radius "10px"
                         :background "#fff"
                         :box-shadow "0 1px 4px rgba(0,0,0,0.1)"
                         :transition "all 0.2s ease"}}
                [:div
                 [:strong {:style {:font-size "1.05em" :color "#333"}}
                  (or (:topic c) "Untitled")]
                 [:div {:style {:font-size "0.85em"
                                :color "#666"
                                :margin-top "0.3em"}}
                  (str "Jumlah Soal: " (count (:problems c)))]]
                [:div {:style {:margin-top "0.75em"
                               :display "flex"
                               :gap "0.6em"}}
                 [:button.btn.btn-sm.btn-success
                  {:style {:flex "1"}
                   :on-click #(do
                                (rf/dispatch [:fetch-custom-proset-by-id (:_id c)])
                                (set! (.-hash js/location)
                                      (str "#practice-custom/" (:_id c))))}
                  "🚀 Mulai"]
                 [:button.btn.btn-sm.btn-danger
                  {:style {:flex "0.7"}
                   :on-click #(when (js/confirm "Lo yakin mau hapus latihan ini?")
                                (rf/dispatch [:delete-custom-proset
                                              (:_id c)
                                              (or (:id user) (:_id user) (:email user))]))}
                  "🗑️ Hapus"]]]))]

           :else
           [:p {:style {:color "#777" :font-style "italic"}}
            "Belum ada latihan gabungan yang lo buat."])]))))


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
  (fn []
    (let [m @(rf/subscribe [:current-material])]
      [:div.container
       [:h2.page-title "🎯 Practice Center"]
       (if (some? material-id)
         [:div
          ;; Box 1 — Info Materi
          [:div.panel
           {:style {:padding "1.5em"
                    :border "1px solid #ddd"
                    :border-radius "10px"
                    :background "#fafafa"
                    :margin-bottom "1.5em"
                    :box-shadow "0 1px 3px rgba(0,0,0,0.08)"}}
           [:h3 {:style {:margin-bottom "0.5em"}}
            (or (:topic m) "Untitled Topic")]
           [:p {:style {:color "#555" :font-size "0.9em"}}
            "Latihan ini bakal bantu lo ningkatin skill di topik ini. "
            "Lo bisa pilih latihan yang udah ada, generate soal baru, "
            "atau langsung tes semua soal dari topik ini."]]

          ;; Box 2 — Aksi utama
          [:div.panel
           {:style {:padding "1.5em"
                    :border "1px solid #ddd"
                    :border-radius "10px"
                    :background "#fff"}}
           [:h3 {:style {:margin-bottom "1em"}} "⚙️ Pilih Aksi Lo"]
           [:div.btn-group {:style {:display "flex" :flex-direction "column" :gap "0.75em"}}
            [:button.btn.btn-primary
             {:on-click #(do
                           (rf/dispatch [:open-bank-soal material-id])
                           (set! (.-hash js/location) "#bank-soal"))}
             "📚 Lihat & Kelola Bank Soal"]
            [:button.btn.btn-accent
             {:on-click #(do
                           (rf/dispatch [:fetch-all-questions material-id])
                           (set! (.-hash js/location)
                                 (str "#practice-all/" material-id)))}
             "🧠 Tes Semua Soal di Materi Ini"]]]

          ;; Box 3 — Motivasi kecil (biar gak kering banget)
          [:div {:style {:margin-top "2em"
                         :text-align "center"
                         :color "#666"
                         :font-style "italic"}}
           [:p "“Semakin sering lo latihan, makin tajem otak lo.” 💪"]]]

         [:div.panel
          [:p.small "Material-id belum ada."]
          [:button.btn.btn-primary
           {:on-click #(rf/dispatch [:fetch-materials])}
           "Refresh daftar materi"]])])))


(defn bank-soal-page []
  (let [ps @(rf/subscribe [:prosets])
        difficulty (r/atom "medium")
        current-material @(rf/subscribe [:current-material])]
    [:div.container
     [:h2.page-title {:style {:margin-bottom "0.5em"
                              :font-weight "700"
                              :font-size "1.6em"
                              :color "#222"}}
      (str "📚 Bank Soal — " (or (:topic current-material) "Untitled"))]
     ;; 📘 Daftar Bank Soal  
     [:div.bank-list
      (doall
       (for [p ps]
         ^{:key (:_id p)}
         [:div.bank-item
          {:style {:border "1px solid #e0e0e0"
                   :border-radius "10px"
                   :padding "1.2em"
                   :margin-bottom "1em"
                   :background "#ffffff"
                   :box-shadow "0 2px 6px rgba(0,0,0,0.05)"
                   :transition "all 0.2s ease"}}
          ;; Header info
          [:div {:style {:display "flex"
                         :justify-content "space-between"
                         :align-items "center"
                         :margin-bottom "0.5em"}}
           [:div
            [:h3 {:style {:margin 0
                          :font-size "1.1em"
                          :color "#333"}}
             (or (:bank-code p)
                 (str "Proset " (:_id p)))]
            [:p {:style {:margin 0
                         :font-size "0.9em"
                         :color "#777"}}
             (str "Difficulty: " (or (:difficulty p) "N/A"))]]]

          ;; Optional jumlah soal
          (when-let [probs (:problems p)]
            [:p {:style {:font-size "0.85em"
                         :color "#555"
                         :margin-bottom "0.8em"}}
             (str "Total Soal: " (count probs))])

          ;; Tombol aksi
          [:div {:style {:display "flex"
                         :gap "0.6em"
                         :margin-top "0.5em"}}
           [:button.btn.btn-sm.btn-primary
            {:style {:flex "1"
                     :font-weight "600"}
             :on-click #(do
                          (rf/dispatch [:fetch-proset-by-id (:_id p)])
                          (set! (.-hash js/location)
                                (str "#practice-proset/" (:_id p))))}
            "💪 Mulai Latihan"]
           [:button.btn.btn-sm.btn-danger
            {:style {:flex "0.6"}
             :on-click #(when (js/confirm "Yakin mau hapus bank soal ini?")
                          (rf/dispatch [:delete-proset (:_id p)]))}
            "🗑️ Hapus"]]]))]

     ;; 💡 Generate Practice Box 
     [:div.panel
      {:style {:padding "1em 1.2em"
               :margin-bottom "1.5em"
               :background "#f9f9f9"
               :border "1px solid #ddd"
               :border-radius "8px"
               :box-shadow "0 1px 3px rgba(0,0,0,0.08)"}}
      [:h3 {:style {:margin-bottom "0.5em"
                    :font-size "1.1em"
                    :color "#333"}}
       "⚙️ Generate Practice Baru"]
      [:p.small {:style {:margin-bottom "0.8em" :color "#555"}}
       "Pilih tingkat kesulitan dan generate soal baru buat materi ini."]

      [:div {:style {:display "flex"
                     :align-items "center"
                     :gap "0.75em"}}
       [:select.form-control
        {:value @difficulty
         :style {:flex "1" :max-width "200px"}
         :on-change #(reset! difficulty (.. % -target -value))}
        [:option {:value "easy"} "Easy"]
        [:option {:value "medium"} "Medium"]
        [:option {:value "hard"} "Hard"]]
       [:button.btn.btn-primary
        {:style {:padding "0.5em 1em"}
         :on-click #(rf/dispatch
                     [:generate-prosets
                      (:_id current-material)
                      @difficulty])}
        "🚀 Generate Practice"]]]]))


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
