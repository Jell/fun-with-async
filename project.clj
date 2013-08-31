(defproject fun-with-async "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.keminglabs/jetty7-websockets-async "0.1.0-SNAPSHOT"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [compojure "1.1.5" :exclusions [ring/ring-core]]
                 [org.clojure/core.match "0.2.0-rc5"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "0.3.2"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"],

  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}

  :cljsbuild {:builds
              [{:source-paths ["src/cljs/fun_with_async"]
                :compiler {:output-to "public/js/fun-with-async.js"
                           :optimizations :whitespace
                           :pretty-print true}}]})
