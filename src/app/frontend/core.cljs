(ns app.frontend.core
  (:require
   [reagent.dom.client :as rdom] 
   [reagent.core :as r]            
   [re-frame.core :as rf]
   [clojure.string :as str]
   [app.frontend.events]
   [app.frontend.subs]
   [app.frontend.views :as views]))

(defonce root (rdom/create-root (.getElementById js/document "app")))

(defn parse-hash []
  (let [h (or (.. js/location -hash) "")]
    (if (str/blank? h)
      [:home]
      (let [v (subs h 1)]
        (cond
          (str/starts-with? v "material/") [:material (subs v 9)]
          (str/starts-with? v "practice-all/") [:practice-all (subs v 13)]
           (= v "practice-all-user")             [:practice-all-user]
          (str/starts-with? v "practice-test/") [:practice-test (subs v 14)]
          (str/starts-with? v "practice/") [:practice (subs v 9)]
          (= v "generate")  [:generate]
          (= v "materials") [:materials]
          (= v "assessment") [:assessment]
          (= v "assessment-test") [:assessment-test]
          (= v "assessment-result") [:assessment-result] 
          (= v "auth")     [:auth]
          :else             [:home])))))

(defn handle-popstate [_]
  (let [[p & params] (parse-hash)]
    (rf/dispatch (vec (concat [:set-page p] params)))))

(defn ^:export init []
  (let [saved (js/localStorage.getItem "user")
        user  (when saved (js/JSON.parse saved))]
    (when user
      (rf/dispatch-sync [:set-user user])
      (rf/dispatch [:fetch-materials (:id user)])))

  ;; init db
  (rf/dispatch-sync [:init])

  ;; initial route dari hash / default
  (let [[p & params] (parse-hash)]
    (if (and (nil? @(rf/subscribe [:user]))
             (= p :home))
      (rf/dispatch [:set-page :auth])
      (rf/dispatch (vec (concat [:set-page p] params)))))

  ;; listen browser back/forward
  (set! (.-onpopstate js/window) handle-popstate)

  ;; render app
  (.render root (r/as-element [views/app-root])))
