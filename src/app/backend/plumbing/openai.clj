(ns app.backend.plumbing.openai
  (:require
    [com.stuartsierra.component :as component]
    [app.backend.utils :as u]
    [clj-http.client :as http]
    [cheshire.core :as json]
    ))

(declare base-request generate transcribe models)

(defrecord Openai [openai-completion openai-key]
  component/Lifecycle
  (start [this]
    (u/info "Setting up the openai component")
    #_(u/pres {:openai-completion openai-completion
               :openai-key openai-key})
    (assoc this
      :openai (fn [{:keys [model messages temperature json-schema] :as foobar}]
                (u/info "Generating from openai")
                (let [send-to-openai {:model      model
                                      :openai-url (str openai-completion)
                                      :messages   messages
                                      :temperature temperature
                                      :json-schema json-schema
                                      :openai-key (str openai-key)
                                      :type       "completion"}]
                  (generate send-to-openai)))))
  (stop [this]
    (u/info "Openai stopped")
    this))

(defn create-openai-component
  "Openai component constructor"
  [{:keys [openai-completion openai-key]}]
  (map->Openai {:openai-completion openai-completion
                :openai-key        openai-key}))

(defn base-request
  [api-token type]
  (condp = type
    "completion" {:accept       :json
                  :content-type :json
                  :headers      {"Authorization" (str "Bearer " api-token)}}
    "tts" {:accept  :json
           ;:content-type :multipart/form-data
           :headers {"Authorization" (str "Bearer " api-token)
                     ;"Content-Type" "multipart/form-data"
                     }}))

(def models
  {:gpt-4        "gpt-4o"
   "gpt-4"       "gpt-4o"
   :gpt-4o-mini  "gpt-4o-mini"
   "gpt-4o-mini" "gpt-4o-mini"
   "whisper-1"   "whisper-1"
   :gpt-5-mini "gpt-5-mini"
   "gpt-5-mini" "gpt-5-mini"
   })


(defn generate
  "Just call this one to generate the response from openAI"
  [{:keys [model openai-url messages temperature json-schema openai-key type] :as send-to-openai}]
  (u/info "Getting into generate function inside openai component")
  (let [data {:model           (models model)
              :messages        messages 
              :response_format (or json-schema {:type "json_object"})
              :max_completion_tokens      128000
              :temperature     temperature
              :n               1}]
    (u/info "Sending to openai...")
    #_ (u/pres data)
    (let [resp (try (->> data
                         (json/generate-string)
                         (assoc (base-request openai-key type) :body)
                         (http/post openai-url))
                    (catch Exception e (u/error e)))]
      ;(u/pres resp)
      (let [resp1 (-> (:body resp)
                      (json/parse-string true))]
        ;(u/pres resp1)
        (-> (select-keys resp1 [:usage])
            (assoc :result (-> (get-in resp1 [:choices 0 :message :content])
                               (json/parse-string true))))))))


(comment

  (keys (:openai @dev/dev-system))

  ((:openai (:openai @dev/dev-system)) {:model "gpt-4o" :messages [{:role    "system"
                                                                    :content "I will always respond with short answer in JSON object"}
                                                                   {:role    "user"
                                                                    :content "What is the meaning of life?"}]})

  )