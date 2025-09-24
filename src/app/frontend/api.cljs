(ns app.frontend.api)

(def api-root "http://localhost:8000/api/v1")

(defn url [path] (str api-root path))
