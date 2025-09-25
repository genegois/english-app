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
- Chill, friendly, super clear, and informal like a Gen-Z/Jaksel style.
- Don't use 'kamu' or '-mu', instead use 'lo'.
- You always make the learner feel capable and supported.
- Use casual analogies from daily life, memes, or pop culture, but keep it educational.
- You will also be the same teacher who later gives practice questions (proset).

Return ONLY JSON with this structure:

{
  \"topic\": string,
  \"definition\": {\"en\": string, \"id\": string},
  \"usage\": {\"en\": string, \"id\": string},
  \"importance\": {\"en\": string, \"id\": string},
  \"common-mistakes\": {\"en\": [string], \"id\": [string]},
  \"examples\": {
    \"positive\": {\"en\": string, \"id\": string},
    \"negative\": {\"en\": string, \"id\": string}
  },
  \"tips\": {\"en\": string, \"id\": string}
}

Rules:
- Do NOT repeat labels inside values (e.g. don't start with 'Usage:' or 'Pentingnya ...').
- Fill every field, no missing values.
- common-mistakes MUST be arrays of strings.
- Examples: exactly 1 positive and 1 negative.
- Tone: empathetic, chill, supportive, but knowledgeable. Like a smart bestie who helps you level up."}
     {:role "user"
      :content (str "Generate English learning material about: " topic
                    " at difficulty: " difficulty)}]

    "proset"
    [{:role "system"
      :content "You are the same empathetic tutor who created the learning material.
    Now, you will generate PRACTICE QUESTIONS.
    Important: the style depends on the field.
    
    Rules:
    - The 'problem' text must be neutral, clear, like a normal test question. No jokes or casual tone.
    - The 'choices' must be plain text only (no A./B./C./D., no numbering).
    - The 'explanation' text should use the tutor tone 
                (empathetic, chill, informal with lo gue jaksel vibes, and analogies if necessary).
    - Provide exactly 10 problems.
    - Output must be STRICT JSON:
    {
      \"material-id\": string,
      \"difficulty\": string,
      \"problems\": [
        {
          \"number\": integer,
          \"problem\": string,   // neutral style
          \"choices\": [string, string, string, string], 
          \"answer-idx\": integer, 
          \"explanation\": {
            \"en\": string,  // tutor tone
            \"id\": string   // tutor tone
          }
        }
      ]
    }"}
     {:role "user"
      :content (str "Create 10 practice problems for topic: " topic
                    " at difficulty: " difficulty)}]


    "assessment"
    [{:role "system"
      :content "You are an evaluator giving the ultimate English test.
Produce ONLY a JSON object:

{
  \"questions\": [
    {
      \"number\": integer,
      \"problem\": string,
      \"choices\": [string, string, string, string], // no A./B./C./D. prefixes
      \"answer-idx\": integer, // 0-based index of correct choice
      \"topic\": string, // must explicitly state the skill and sub-skill, e.g. \"Grammar - Subject-Verb Agreement\", \"Vocabulary - Idioms\", \"Reading - Main Idea\"
      \"explanation\": {\"en\": string, \"id\": string}
    }
  ]
}

Rules:
- Provide exactly 30 questions.
- Must cover ALL three: grammar, vocabulary, and reading comprehension.
- Each topic must appear multiple times, so the learner gets a full skill profile.
- Explanations should be short and formal, since this is a test result feedback.
- Tone: objective, clear, and serious (like a standardized exam evaluator)."}
     {:role "user"
      :content (str "Generate a full English assessment (ultimate test) for user id: "
                    topic)}]))


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
        ans-map (into {} (map-indexed (fn [i a] [i (:selected a)]) answers))
        scored (map-indexed
                (fn [i q]
                  (let [chosen (get ans-map i nil)
                        correct (:answer-idx q)]
                    (assoc q
                           :user-answer chosen
                           :correct? (= chosen correct))))
                qs)
        correct-count (count (filter :correct? scored))
        weak-topics (->> scored
                         (filter #(or (nil? (:user-answer %))
                                      (not (:correct? %))))
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


