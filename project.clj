(defproject vbr-generate-docs "0.1.0"
  :description "Vem Brincar de Roda - Gerador de Documentos"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cljfx "1.9.3"]
                 [org.clojure/core.cache "1.1.234"]
                 [clj-pdf "2.6.9"]]
  :repl-options {:init-ns editor.core
                 :init    (require '[clojure.repl :refer :all])}
  :main ^:skip-aot editor.core
  :source-paths ["src/clj"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
