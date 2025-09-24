(ns app.backend.features.english
  (:require [monger.collection :as mc]
            [app.backend.utils :as u]))

;; helper: build OpenAI messages
(defn english-generator-messages [topic difficulty mode]
  (case mode
    "material"
    [{:role "system"
      :content "You are an empathetic and super-smart English tutor.
Your vibe:
- Chill, friendly, and super clear like a Gen-Z/Jaksel style.
- Don't use 'kamu' or '-mu' instead use 'lo'.
- You always make the learner feel capable and supported.
- Use casual analogies from daily life, memes, or pop culture, but keep it educational.

Return ONLY JSON. Follow this structure exactly:

{
  \"topic\": string,
  \"definition\": {\"en\": string, \"id\": string},
  \"usage\": {\"en\": string, \"id\": string},
  \"importance\": {\"en\": string, \"id\": string},
  \"common-mistakes\": {\"en\": [string], \"id\": [string]},
  \"examples\": {\"positive\": {\"en\": string, \"id\": string},
                 \"negative\": {\"en\": string, \"id\": string}},
  \"tips\": {\"en\": string, \"id\": string}
}

Rules:
- Do NOT repeat labels like 'Usage:' or 'Pentingnya apaan' inside the values. Just write the explanations directly.
- Always fill every field, no missing values.
- common-mistakes MUST be arrays of strings.
- Examples must always have one positive and one negative sentence.
- Tone: empathetic, chill, supportive, but very knowledgeable. Like a smart bestie who helps you level up without making you feel dumb."}
     {:role "user"
      :content (str "Generate English learning material about: " topic " at difficulty: " difficulty)}]

    "proset"
    [{:role "system"
      :content "Produce a JSON object with:
                material-id,
                difficulty,
                problems: array of 10 objects.
                Each problem: {number, problem (string), choices [A-D], answer-idx, explanation {en, id}}.
                Tone: clear and effective, fokus ke latihan belajar."}
     {:role "user"
      :content (str "Create 10 practice problems for topic: " topic " at difficulty: " difficulty)}]

    "assessment"
    [{:role "system"
      :content "Produce a JSON object with:
                    questions: array of 15 objects.
                    Each question: {number, problem, options [A-D], answer-idx, topic (string), explanation {en, id}}.
                    Cover grammar, vocabulary, and reading.
                    Tone: clear and effective, fokus ke evaluasi belajar."}
     {:role "user"
      :content (str "Generate a full assessment for user id: " topic)}]))

;; ------------------------
;; PROSETS
;; ------------------------
(defn generate-proset! [db openai-comp material-id difficulty]
  (let [material (mc/find-one-as-map db "materials" {:_id material-id})
        topic (:topic material)
        chosen-diff (or difficulty (:difficulty material) "medium")
        existing-count (mc/count db "prosets" {:material-id material-id})
        messages (english-generator-messages topic chosen-diff "proset")
        gen-fn (:openai openai-comp)
        resp (gen-fn {:model "gpt-5-mini"
                      :messages messages
                      :temperature 1})
        content (u/parse-json (:result resp))
        problems (:problems content)
        proset {:_id        (u/uuid)
                :material-id material-id
                :topic      topic
                :difficulty chosen-diff
                :bank-code  (str "bank-soal-" (inc existing-count))
                :problems   problems
                :created-at (u/now)}]
    (mc/insert-and-return db "prosets" proset)
    (u/info proset)
    proset))


(defn fetch-prosets [db material-id]
  (mc/find-maps db "prosets" {:material-id material-id}))

(defn submit-proset! [db proset-id answers]
  (let [proset (mc/find-one-as-map db "prosets" {:_id proset-id})
        qs (:problems proset)
        total (count qs)
        scored (map (fn [q ans]
                      (let [correct (:answer-idx q)
                            chosen (:selected ans)
                            ok? (= correct chosen)]
                        (assoc q :user-answer chosen :correct? ok?)))
                    qs answers)
        correct-count (count (filter :correct? scored))]
    (mc/update db "prosets"
               {:_id proset-id}
               {"$set" {:problems scored
                        :score {:correct correct-count :total total}}})
    (mc/find-one-as-map db "prosets" {:_id proset-id})))

(defn grade-problems [problems answers]
  (let [scored (map-indexed
                (fn [i p]
                  (let [user-ans (get-in answers [i :selected])
                        correct-idx (:answer-idx p)
                        correct? (= user-ans correct-idx)]
                    (assoc p
                           :user-answer user-ans
                           :correct? correct?)))
                problems)
        correct (count (filter :correct? scored))
        total   (count problems)]
    {:score {:correct correct
             :total total}
     :problems (vec scored)}))


;; ------------------------
;; MATERIALS
;; ------------------------
(defn generate-material! [db openai-comp user-id topic difficulty]
  (let [gen-fn   (:openai openai-comp)
        messages (english-generator-messages topic difficulty "material")
        resp     (gen-fn {:model "gpt-5-mini"
                          :messages messages
                          :temperature 1})
        content  (u/parse-json (:result resp))
        doc      (merge {:_id (u/uuid)
                         :user-id user-id
                         :topic topic
                         :difficulty difficulty}
                        {:content content})
        inserted (mc/insert-and-return db "materials" doc)]
    (generate-proset! db openai-comp (:_id inserted) difficulty)
    (u/info doc)
    inserted))

(defn fetch-materials [db user-id]
  (mc/find-maps db "materials" {:user-id user-id}))

(defn fetch-material-by-id [db material-id]
  (mc/find-one-as-map db "materials" {:_id material-id}))

;; ------------------------
;; ASSESSMENTS
;; ------------------------
(defn generate-assessment! [db openai-comp user-id]
  (let [gen-fn   (:openai openai-comp)
        messages (english-generator-messages
                  "grammar, vocabulary, reading"
                  "mixed"
                  "assessment")
        resp     (gen-fn {:model "gpt-5-mini"
                          :messages messages
                          :temperature 1})
        content  (u/parse-json (:result resp))
        doc {:_id (u/uuid)
             :user-id user-id
             :questions (:questions content)
             :topics-covered ["grammar" "vocabulary" "reading"]
             :weak-topics []
             :created-at (u/now)}]
    (mc/insert-and-return db "assessments" doc)
    (u/info doc)
    doc))

(defn submit-assessment! [db _ assessment-id answers]
  (let [assessment (mc/find-one-as-map db "assessments" {:_id assessment-id})
        qs (:questions assessment)
        total (count qs)
        scored (map-indexed
                (fn [i q]
                  (let [chosen (:selected (nth answers i nil))
                        correct (:answer-idx q)]
                    (assoc q
                           :user-answer chosen
                           :correct? (= chosen correct))))
                qs)
        correct-count (count (filter :correct? scored))
        weak-topics (->> scored
                         (filter (comp not :correct?))
                         (map #(or (:topic %) "misc"))
                         distinct
                         vec)
        result {:questions scored
                :score {:correct correct-count :total total}
                :weak-topics weak-topics}]
    (mc/update db "assessments"
               {:_id assessment-id}
               {"$set" result})
    (assoc assessment
           :questions scored
           :score {:correct correct-count :total total}
           :weak-topics weak-topics)))

