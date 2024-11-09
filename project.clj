(defproject vbr-generate-docs "1.0"
  :description "Vem Brincar de Roda - Gerador de Documentos"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.cache "1.1.234"]
                 [cljfx "1.9.3"]
                 [clj-pdf "2.6.9"]]
  :repl-options {:init-ns editor.core
                 :init    (require '[clojure.repl :refer :all])}
  :main editor.core
  :target-path "dist"
  :uberjar-name "vbr-generate-docs.jar"
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dcljfx.skip-javafx-initialization=true"]}})
