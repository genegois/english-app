(defproject app "0.1.0"
  :dependencies
  [;; basic clojure setup and app management
   [org.clojure/clojure "1.11.1"]
   [com.stuartsierra/component "1.1.0"]
   [prismatic/schema "1.4.1"]
   [org.clojure/tools.namespace "0.2.4"]

   ;; basic web plumbing & web needs
   [org.immutant/web "2.1.10" :exclusions [commons-codec]]
   [clj-http "3.12.3"]
   [ring/ring-defaults "0.3.4" :exclusions [commons-codec]]
   [jumblerg/ring-cors "3.0.0"]
   [ring/ring-json "0.5.1"]
   [ring "1.9.6" :exclusions [commons-codec]]
   [ring-cors "0.1.13"]
   [ring/ring-json "0.5.1"]
   [ring/ring-anti-forgery "1.3.0"]
   [metosin/reitit "0.6.0"]
   [selmer "1.12.56" :exclusions [commons-codec]]

   ;; database & data processing
   [com.novemberain/monger "3.6.0"]
   [cheshire "5.11.0"]
   [com.github.seancorfield/next.jdbc "1.3.909"]
   [org.postgresql/postgresql "42.7.2"]
   [com.zaxxer/HikariCP "5.0.1"]
   [com.github.seancorfield/honeysql "2.2.891"]
   [org.clojure/data.csv "1.1.0"]
   [msolli/proletarian "1.0.68-alpha"]
   [dev.weavejester/ragtime "0.9.2"]

   ;; encryption
   [buddy/buddy-core "1.11.423"]
   [buddy/buddy-sign "3.5.351"]
   [buddy/buddy-hashers "2.0.167"]
   [com.google.api-client/google-api-client "1.32.1"]

   ;; utilities
   [com.taoensso/timbre "6.1.0"]
   [environ "1.2.0"]
   [clojure.java-time "1.2.0"]
   ;; [me.raynes/fs "1.4.6"]
   [danlentz/clj-uuid "0.1.9"]

   ;; file/formatting and development utilities
   [org.clojure/tools.namespace "1.4.4"]
   [pjstadig/humane-test-output "0.11.0"]
   [ring/ring-mock "0.4.0"]

   ;; logging to prevent the logback error
   [ch.qos.logback/logback-classic "1.2.3"]
   [org.slf4j/slf4j-api "1.7.30"]
   
   ;; FE 
   [org.clojure/clojurescript "1.11.60"] 
   [reagent "1.2.0"]
   [re-frame "1.4.0"]
   [thheller/shadow-cljs "2.28.3"] 
   [cljs-ajax "0.8.4"]
   [day8.re-frame/http-fx "0.2.4"]]

  :injections [(require 'pjstadig.humane-test-output)
               (pjstadig.humane-test-output/activate!)]

  :uberjar-name "uberjar-app.jar"
  :jar-name "appstore.jar"

  :min-lein-version "2.5.3"

  :source-paths ["src" "dev"]

  :resource-paths ["resources"]
  :main ^:skip-aot app.core
  :repl-options {:init-ns user}

  :plugins [[lein-environ "1.2.0"]
            [lein-shadow "0.4.0"]]
  ;; :pom-plugins [[com.google.cloud.tools/jib-maven-plugin "2.1.0"
  ;;                (:configuration
  ;;                 [:from [:image "clojure:temurin-8-lein-jammy"]]
  ;;                 [:container
  ;;                  [:mainClass "app.core"]
  ;;                  [:creationTime "USE_CURRENT_TIMESTAMP"]])]]
  
  :profiles {:dev           [:project/dev :profiles/dev]
             :test          [:project/test :profiles/test]
             ;; only edit :profiles/* in profiles.clj
             :profiles/dev  {}
             :profiles/test {}
             :project/dev   {:source-paths ["src" "dev"]}
             :project/test  {:source-paths ["src" "dev"]}
             :uberjar       {:aot          :all
                             :source-paths ["src"]
                             :main         app.core}}
  :shadow-cljs
  {:builds
   {:app {:target :browser
          :output-dir "resources/public/js"
          :asset-path "/js"
          :modules {:main {:init-fn app.frontend.core/init}}
          :devtools {:http-root "resources/public"
                     :http-port 8020}}}})