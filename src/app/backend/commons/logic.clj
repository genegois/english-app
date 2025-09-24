(ns app.backend.commons.logic
  (:require [app.backend.utils :as u]))

(defn generic-ordering
  "Ordering a list of maps based on the order of their ids"
  [entities ordered-ids]
  (loop [[id & ids] ordered-ids res []]
    (if id
      (let [entity (first (filter #(= (:_id %) id) entities))]
        (recur ids (conj res entity)))
      res)))
