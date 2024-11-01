(ns editor.core
  (:gen-class)
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [clojure.string :as str]
            [editor.components :as component])
  (:use [clj-pdf.core])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)
           (java.util Locale)
           (javafx.application Platform)
           (javafx.scene Node)
           (javafx.scene.control ToggleGroup)
           (javafx.scene.input KeyCode KeyEvent MouseEvent)
           (javafx.stage DirectoryChooser)))

(defn get-version [& _]
  (-> "project.clj" slurp read-string (nth 2)))

(def *state
  (atom (fx/create-context
          {:data          {:name           "Fulano da Silva de Oliveira"
                           :cpf            "12345678901"
                           :date           (LocalDate/now)
                           :min_age        "2"
                           :max_age        "12"
                           :time_start     "18:00"
                           :time_end       "21:00"
                           :num_kids       "10"
                           :num_pros       "2"
                           :total          ""
                           :down_payment   ""
                           :activities     [{:id 0, :activity "Massinha de Modelar", :checked false}
                                            {:id 1, :activity "Música", :checked false}
                                            {:id 2, :activity "Pintura Artística", :checked false}
                                            {:id 3, :activity "Pintura Facial", :checked false}
                                            {:id 4, :activity "Recreação", :checked false}
                                            {:id 5, :activity "Teatro", :checked false}]
                           :workshops      [{:id 0, :workshop "Circo", :checked false}
                                            {:id 1, :workshop "Fazendinha", :checked false}
                                            {:id 2, :workshop "Princesa", :checked false}
                                            {:id 3, :workshop "Super-Herói", :checked false}
                                            {:id 4, :workshop "Unicórnio", :checked false}]
                           :extra-activity ""
                           :extra-workshop ""}
           :edit-activity nil
           :edit-workshop nil
           :errors        {}}
          #(cache/lru-cache-factory % :threshold 4096))))

; Global state
(defn generate-pdf [data path]
  (pdf
    [{:font {:size 12 :family :justified}}
     [:image {:scale 15 :align :center} "resources/logo.png"]
     [:paragraph {:align :center :style :bold} "CONTRATO DE PRESTAÇÃO DE SERVIÇOS"]
     [:spacer]
     [:paragraph {:style :bold} "DAS PARTES"]
     [:spacer]
     [:paragraph
      [:chunk {:style :bold} "CONTRATANTE: "] (:name data)
      "   "
      [:chunk {:style :bold} "CPF: "] (:cpf data)]
     [:paragraph
      [:chunk {:style :bold} "CONTRATADA: "] "27.831.172 MERIELLE GILBERTA DE OLIVEIRA"
      "   "
      [:chunk {:style :bold} "CNPJ: "] "27.831.172/0001-63"]
     [:spacer]
     [:paragraph
      [:chunk {:style :bold} "CLÁUSULA PRIMEIRA – "]
      [:chunk {:style :underline} "DO OBJETO:"]]
     [:paragraph "O presente contrato tem por objetivo a prestação de serviços especializados em recreação infantil por parte da CONTRATADA de acordo com os termos e condições detalhados neste contrato."]
     [:spacer]
     [:paragraph [:chunk {:style :bold} "DATA DA FESTA: "] (str (.format (DateTimeFormatter/ofPattern "dd/MM/yyyy") (:date data)))]
     [:paragraph [:chunk {:style :bold} "HORÁRIO DA FESTA: "] (:time_start data) " às " (:time_end data)]
     [:paragraph [:chunk {:style :bold} "ENDEREÇO DA FESTA: "] (:address data)]
     [:paragraph [:chunk {:style :bold} "QUANTIDADE DE CRIANÇAS: "] (:num_kids data)]
     [:paragraph [:chunk {:style :bold} "IDADE MÉDIA: "] (:min_age data) " a " (:max_age data) " anos"]
     [:paragraph [:chunk {:style :bold} "NOME DO ANIVERSARIANTE: "] (:kid data)]
     [:paragraph [:chunk {:style :bold} "TEMA DA FESTA: "] (:theme data)]
     [:spacer]
     [:paragraph
      [:chunk {:style :bold} "CLÁUSULA SEGUNDA – "]
      [:chunk {:style :underline} "DOS SERVIÇOS:"]]
     [:paragraph (str "Serviços de recreação: " (:num_pros data) (if (= (:num_pros data) "1") " profissional" " profissionais"))]
     (when (seq (filter :checked (:activities data)))
       [:paragraph (str "Atividades: ") (str/join ", " (map :activity (filter :checked (:activities data))))])
     (when (seq (filter :checked (:workshops data)))
       [:paragraph (str "Oficinas: ") (str/join ", " (map :workshop (filter :checked (:workshops data))))])
     (when (seq (:extra_items data))
       [:paragraph (str "Outros: ") (:extra_items data)])
     [:spacer]
     [:paragraph [:chunk {:style :bold} "CLÁUSULA TERCEIRA – "] [:chunk {:style :underline} "DO PREÇO E CARGA HORÁRIA:"]]
     [:paragraph "O valor total do presente contrato é de R$ " [:chunk {:style :bold} (:total data)] " (por 3h de recreação)"]
     [:spacer]
     [:paragraph [:chunk {:style :bold} "CLÁUSULA QUARTA – "] [:chunk {:style :underline} "DAS CONDIÇÕES DE PAGAMENTO:"]]
     [:paragraph "A CONTRATANTE pagará a título de entrada o valor de R$ " (:down_payment data) " via PIX, e o restante no dia da festa."]
     [:spacer]
     [:paragraph [:chunk {:style :bold} "CLÁUSULA QUINTA – "] [:chunk {:style :underline} "DO CANCELAMENTO:"]]
     [:paragraph "O cancelamento implicará em multa de 50% do valor total do contrato. O não cumprimento pela CONTRATADA na data do evento resultará em multa de 50% do valor do contrato, além da devolução integral do sinal."]
     [:spacer]
     [:paragraph [:chunk {:style :bold} "CLÁUSULA SEXTA – "] [:chunk {:style :underline} "DA MUDANÇA DE HORÁRIOS:"]]
     [:paragraph "Fica estipulado o prazo mínimo de uma semana para aviso de mudança no horário acordado em contrato da festa. Solicitações de mudança sem aviso prévio de uma semana, poderão acarretar no não cumprimento do serviço por parte da CONTRATADA."]
     [:spacer]
     [:paragraph [:chunk {:style :bold} "CLÁUSULA SÉTIMA – "] [:chunk {:style :underline} "DA ALIMENTAÇÃO:"]]
     [:paragraph "Os profissionais terão direito à alimentação do buffet da festa. Caso haja tal impossibilidade, fica sujeito a cobrança de taxa adicional a ser discutida previamente à assinatura do contrato."]
     [:spacer 2]
     [:paragraph "São Paulo, " (str (.format (DateTimeFormatter/ofPattern "dd 'de' MMMM 'de' yyyy" (Locale/forLanguageTag "pt-BR")) (LocalDate/now)))]
     [:spacer 2]
     [:line]
     [:paragraph "Assinatura da CONTRATANTE"]
     [:spacer 2]
     [:line]
     [:paragraph "Assinatura da CONTRATADA"]]
    (format "%s/Contrato - %s.pdf" path (:name data))))

;; Subscriptions

(defn- all-ids-sub [ctx prop]
  (map :id (fx/sub-val ctx get-in [:data prop])))

(defn- value-sub [ctx prop id attr]
  (some #(when (= (:id %) id) (attr %)) (fx/sub-val ctx get-in [:data prop])))

;; Views
(defn client-type-selection [_]
  (let [toggle-group (ToggleGroup.)]
    {:fx/type  :h-box
     :spacing  20
     :padding  {:bottom 20}
     :children [{:fx/type      :radio-button
                 :text         "Pessoa Física"
                 :selected     true
                 :toggle-group toggle-group}
                {:fx/type      :radio-button
                 :text         "Empresa"
                 :disable      true
                 :toggle-group toggle-group}]}))

(defn contractor-section [{:keys [fx/context]}]
  (let [cpf (fx/sub-val context get-in [:data :cpf])]
    {:fx/type  :v-box
     :children [{:fx/type  component/form-row
                 :children [{:fx/type     component/labeled-input
                             :label       "Contratante:"
                             :id          :name
                             :h-box/hgrow :always}
                            {:fx/type            component/labeled-input
                             :label              "CPF:"
                             :id                 :cpf
                             :h-box/hgrow        :always
                             :on-focused-changed {:event/type    ::blur
                                                  :id            :cpf
                                                  :handler       component/valid-cpf?
                                                  :error-message "CPF inválido"}
                             :text-formatter     {:fx/type         :text-formatter
                                                  :value-converter component/cpf-value-converter
                                                  :filter          component/cpf-value-filter
                                                  :value           cpf}}]}]}))

(defn party-details-section [{:keys [fx/context]}]
  (let [data (fx/sub-val context :data)]
    {:fx/type  :v-box
     :children [{:fx/type  component/form-row
                 :children [{:fx/type component/labeled-date-picker
                             :id      :date
                             :label   "Data:"
                             :value   (:date data)}
                            {:fx/type    :v-box
                             :fill-width true
                             :children   [{:fx/type :label
                                           :text    "Horário:"}
                                          {:fx/type  :h-box
                                           :spacing  10
                                           :children [{:fx/type         :text-field
                                                       :max-width       60
                                                       :on-text-changed {:event/type ::type :id :time_start}
                                                       :text-formatter  {:fx/type         :text-formatter
                                                                         :value-converter component/time-value-converter
                                                                         :filter          component/time-value-filter
                                                                         :value           (:time_start data)}}
                                                      {:fx/type   :label
                                                       :text      "às"
                                                       :padding   {:top 8}
                                                       :alignment :bottom-center}
                                                      {:fx/type         :text-field
                                                       :max-width       60
                                                       :text            "21:00"
                                                       :on-text-changed {:event/type ::type :id :time_end}
                                                       :text-formatter  {:fx/type         :text-formatter
                                                                         :value-converter component/time-value-converter
                                                                         :filter          component/time-value-filter
                                                                         :value           (:time_end data)}}]}]}]}

                {:fx/type  component/form-row
                 :children [{:fx/type     component/labeled-input
                             :label       "Aniversariante:"
                             :id          :kid
                             :h-box/hgrow :always}
                            {:fx/type     component/labeled-input
                             :label       "Tema:"
                             :id          :theme
                             :h-box/hgrow :always}]}

                {:fx/type  component/form-row
                 :children [{:fx/type     component/labeled-input
                             :label       "Endereço da Festa:"
                             :id          :address
                             :h-box/hgrow :always}]}]}))

(defn- editable-cell [{:keys [fx/context id prop edit-prop attr value-converter]}]
  (let [edit (fx/sub-val context edit-prop)
        value (fx/sub-ctx context value-sub prop id attr)]
    (if (= edit [id attr])
      {:fx/type        :text-field
       :on-key-pressed {:event/type ::key-pressed :edit-prop edit-prop}
       :text-formatter {:fx/type          :text-formatter
                        :value-converter  value-converter
                        :value            value
                        :on-value-changed {:event/type ::edit-description
                                           :prop       prop
                                           :edit-prop  edit-prop
                                           :attr       attr
                                           :id         id}}}
      {:fx/type          :label
       :on-mouse-clicked {:event/type ::on-cell-click :edit-prop edit-prop :id id :attr attr}
       :text             (str value)})))

(defn- make-attr-cell-factory [view prop edit-prop attr value-converter]
  ;; cell factory receives item and has to return prop map without :fx/type to satisfy
  ;; javafx cell renderer which has to establish type of cell beforehand. We still want to
  ;; access context, so we have to use :graphic property which can be a normal component
  (fn [id]
    {:text    ""
     :graphic {:fx/type         view
               :id              id
               :prop            prop
               :edit-prop       edit-prop
               :attr            attr
               :value-converter value-converter}}))

(defn- checkbox-cell [{:keys [fx/context id prop attr]}]
  (let [checked (fx/sub-ctx context value-sub prop id attr)]
    {:fx/type             :check-box
     :selected            (boolean checked)
     :on-selected-changed {:event/type ::toggle-selected :prop prop :id id}}))

(def workshop-select-cell-factory (make-attr-cell-factory checkbox-cell :workshops :edit-workshop :checked :default))
(def workshop-description-cell-factory (make-attr-cell-factory editable-cell :workshops :edit-workshop :workshop :default))

(def activity-select-cell-factory (make-attr-cell-factory checkbox-cell :activities :edit-activity :checked :default))
(def activity-description-cell-factory (make-attr-cell-factory editable-cell :activities :edit-activity :activity :default))

(defn- selectable-table [{:keys [fx/context title select-cell-factory description-cell-factory prop]}]
  {:fx/type              :table-view
   :editable             true
   :column-resize-policy :constrained
   :max-height           200
   :columns              [{:fx/type            :table-column
                           :max-width          30
                           :sortable           false
                           :style              {:-fx-alignment "center"}
                           :cell-value-factory identity
                           :cell-factory       {:fx/cell-type :table-cell
                                                :describe     select-cell-factory}}
                          {:fx/type            :table-column
                           :text               title
                           :sortable           false
                           :cell-value-factory identity
                           :style              {:-fx-padding "5"}
                           :cell-factory       {:fx/cell-type :table-cell
                                                :describe     description-cell-factory}}]
   :items                (fx/sub-ctx context all-ids-sub prop)})

(defn- add-extra-input [{:keys [fx/context prompt-text prop on-text-changed on-key-pressed on-add-action]}]
  {:fx/type  :h-box
   :spacing  10
   :padding  {:top 10}
   :children [{:fx/type         :text-field
               :prompt-text     prompt-text
               :h-box/hgrow     :always
               :text            (fx/sub-val context get-in [:data prop])
               :on-text-changed on-text-changed
               :on-key-pressed  on-key-pressed}
              {:fx/type   :button
               :text      "Adicionar"
               :on-action on-add-action}]})

(defn package-section [_]
  {:fx/type  :v-box
   :children [{:fx/type  component/form-row
               :children [{:fx/type        component/labeled-input
                           :label          "Qtd Crianças:"
                           :id             :num_kids
                           :max-width      80
                           :text-formatter (component/numeric-formatter 4)}

                          {:fx/type    :v-box
                           :fill-width true
                           :children   [{:fx/type :label
                                         :text    "Idade Média:"}
                                        {:fx/type  :h-box
                                         :spacing  10
                                         :children [{:fx/type         :text-field
                                                     :max-width       60
                                                     :prompt-text     "Min"
                                                     :on-text-changed {:event/type ::type :id :min_age}
                                                     :text-formatter  (component/numeric-formatter 2)}
                                                    {:fx/type   :label
                                                     :text      "a"
                                                     :padding   {:top 8}
                                                     :alignment :bottom-center}
                                                    {:fx/type         :text-field
                                                     :max-width       60
                                                     :prompt-text     "Max"
                                                     :on-text-changed {:event/type ::type :id :max_age}
                                                     :text-formatter  (component/numeric-formatter 2)}]}]}

                          {:fx/type        component/labeled-input
                           :label          "N. Profissionais:"
                           :id             :num_pros
                           :max-width      80
                           :h-box/hgrow    :always
                           :text-formatter (component/numeric-formatter 2)}]}

              {:fx/type  component/form-row
               :children [{:fx/type     :v-box
                           :fill-width  true
                           :h-box/hgrow :always
                           :children    [{:fx/type                  selectable-table
                                          :title                    "Atividades"
                                          :prop                     :activities
                                          :select-cell-factory      activity-select-cell-factory
                                          :description-cell-factory activity-description-cell-factory}
                                         {:fx/type         add-extra-input
                                          :prompt-text     "Outra Atividade"
                                          :prop            :extra-activity
                                          :on-text-changed {:event/type ::set-extra
                                                            :extra-prop :extra-activity}
                                          :on-key-pressed  {:event/type ::add-property-enter
                                                            :prop       :activities
                                                            :attr       :activity
                                                            :extra-prop :extra-activity}
                                          :on-add-action   {:event/type ::add-property-action
                                                            :prop       :activities
                                                            :attr       :activity
                                                            :extra-prop :extra-activity}}]}
                          {:fx/type     :v-box
                           :fill-width  true
                           :h-box/hgrow :always
                           :children    [{:fx/type                  selectable-table
                                          :title                    "Oficinas"
                                          :prop                     :workshops
                                          :select-cell-factory      workshop-select-cell-factory
                                          :description-cell-factory workshop-description-cell-factory}
                                         {:fx/type         add-extra-input
                                          :prompt-text     "Outra Oficina"
                                          :prop            :extra-workshop
                                          :on-text-changed {:event/type ::set-extra
                                                            :extra-prop :extra-workshop}
                                          :on-key-pressed  {:event/type ::add-property-enter
                                                            :prop       :workshops
                                                            :attr       :workshop
                                                            :extra-prop :extra-workshop}
                                          :on-add-action   {:event/type ::add-property-action
                                                            :prop       :workshops
                                                            :attr       :workshop
                                                            :extra-prop :extra-workshop}}]}]}

              {:fx/type  :v-box
               :spacing  10
               :children [{:fx/type :label
                           :text    "Itens Extra:"}
                          {:fx/type  component/form-row
                           :children [
                                      {:fx/type         :text-area
                                       :max-height      100
                                       :prompt-text     "Atividades de parceiros, brinquedos, etc."
                                       :on-text-changed {:event/type ::type :id :extra_items}
                                       :h-box/hgrow     :always}]}]}]})

(defn values-section [{:keys [fx/context]}]
  (let [total (fx/sub-val context get-in [:data :total])
        down_payment (fx/sub-val context get-in [:data :down_payment])]
    {:fx/type   :v-box
     :spacing   10
     :alignment :bottom-right
     :children  [{:fx/type     :combo-box
                  :items       ["Pix", "Crédito", "Débito"]
                  :prompt-text "Forma de Pagamento"}

                 {:fx/type        component/labeled-input
                  :label          "Total:"
                  :id             :total
                  :style          {:-fx-font-size    16
                                   :-fx-font-weight  "bold"
                                   :-fx-border-color "green"
                                   :-fx-padding      "10 14"}
                  :text-formatter (component/currency-text-formatter)}

                 {:fx/type        component/labeled-input
                  :label          "Entrada:"
                  :id             :down_payment
                  :style          {:-fx-font-size   14
                                   :-fx-font-weight "bold"
                                   :-fx-padding     "8 14"}
                  :text-formatter {:fx/type         :text-formatter
                                   :value-converter :default
                                   :filter          component/currency-value-filter
                                   :value           down_payment}}

                 {:fx/type :label
                  :id      "down_payment_percentage"
                  :text    (component/calculate-percentage down_payment total)}]}))

(defn export-pdf-button [_]
  {:fx/type   :v-box
   :alignment :center
   :children  [{:fx/type   :button
                :text      "Exportar PDF"
                :style     {:-fx-font-weight "bold"
                            :-fx-padding     "10 20"}
                :on-action {:event/type ::export-doc}}]})

(defn tabs-view [_]
  {:fx/type :tab-pane
   :tabs    [{:fx/type component/tab
              :label   "Novo Contrato"
              :content {:fx/type  :v-box
                        :padding  30
                        :children [{:fx/type client-type-selection}
                                   {:fx/type contractor-section}
                                   {:fx/type component/section-divider :text "DADOS DA FESTA"}
                                   {:fx/type party-details-section}
                                   {:fx/type component/section-divider :text "DADOS DO PACOTE"}
                                   {:fx/type package-section}
                                   {:fx/type component/section-divider :text "FINANCEIRO"}
                                   {:fx/type values-section}
                                   {:fx/type export-pdf-button}]}}
             {:fx/type component/tab
              :label   "Novo Orçamento"
              :content {:fx/type   :h-box
                        :alignment :center
                        :spacing   50
                        :children  [{:fx/type :label
                                     :text    "Em construção 🏗"}]}}]})

(defn root-view [_]
  (let [version (get-version)]
    {:fx/type :stage
     :showing true
     :title   (str "Gerador de Documentos v" version)
     :width   700
     :height  800
     :scene   {:fx/type :scene
               :root    {:fx/type      :scroll-pane
                         :fit-to-width true
                         :content      {:fx/type tabs-view}}}}))

;; Event handlers

(defmulti event-handler :event/type)

(defmethod event-handler :default [e]
  (prn (:event/type e) (:fx/event e) (dissoc e :fx/context :fx/event :event/type)))

(defmethod event-handler ::on-cell-click [{:keys [fx/context ^MouseEvent fx/event edit-prop id attr]}]
  (when (= 2 (.getClickCount event))
    {:context (fx/swap-context context assoc edit-prop [id attr])}))

(defmethod event-handler ::edit-description [{:keys [fx/context fx/event id prop edit-prop attr]}]
  (let [property-list (fx/sub-val context get-in [:data prop])]
    {:context (fx/swap-context context
                               (fn [ctx]
                                 (-> ctx
                                     (assoc-in [:data prop]
                                               (mapv (fn [prop]
                                                       (if (= (:id prop) id)
                                                         (assoc prop attr event)
                                                         prop))
                                                     property-list))
                                     (assoc edit-prop nil))))}))

(defmethod event-handler ::key-pressed [{:keys [fx/context ^KeyEvent fx/event edit-prop]}]
  (when (= (.getCode event) KeyCode/ESCAPE)
    {:context (fx/swap-context context dissoc edit-prop)}))

(defmethod event-handler ::toggle-selected [{:keys [fx/context prop id fx/event]}]
  (let [property-list (fx/sub-val context get-in [:data prop])]
    {:context (fx/swap-context context
                               (fn [ctx]
                                 (assoc-in ctx [:data prop]
                                           (mapv (fn [prop]
                                                   (if (= (:id prop) id)
                                                     (assoc prop :checked event)
                                                     prop))
                                                 property-list))))}))

(defmethod event-handler ::set-extra [{:keys [fx/context fx/event extra-prop]}]
  {:context (fx/swap-context context assoc-in [:data extra-prop] event)})

(defn- add-property [context prop attr extra-prop]
  (let [extra-desc (fx/sub-val context get-in [:data extra-prop])]
    (when (seq extra-desc)
      {:context (fx/swap-context context
                                 (fn [ctx]
                                   (-> ctx
                                       (update-in [:data prop]
                                                  conj {:id      (count (get-in ctx [:data prop]))
                                                        attr     extra-desc
                                                        :checked true})
                                       (assoc-in [:data extra-prop] ""))))})))

(defmethod event-handler ::add-property-action [{:keys [fx/context prop attr extra-prop]}]
  (add-property context prop attr extra-prop))

(defmethod event-handler ::add-property-enter [{:keys [fx/context ^KeyEvent fx/event prop attr extra-prop]}]
  (when (= (.getCode event) KeyCode/ENTER)
    (add-property context prop attr extra-prop)))

(defn- validate-fields [data required-fields]
  (reduce (fn [errors field]
            (if (empty? (get data field))
              (assoc errors field "Campo obrigatório")
              errors))
          {}
          required-fields))

(defmethod event-handler ::export-doc [{:keys [fx/context fx/event]}]
  (let [data (fx/sub-val context :data)
        required-fields [:name :cpf :time_start :time_end :kid :num_kids :num_pros :theme :address :total :down_payment]
        validation-errors (validate-fields data required-fields)]
    (if (empty? validation-errors)
      (do
        (Platform/runLater
          (fn []
            (let [directory-chooser (doto (DirectoryChooser.)
                                      (.setTitle "Selecionar diretório"))
                  window (.getWindow (.getScene ^Node (.getTarget event)))
                  selected-dir (.showDialog directory-chooser window)]
              (if selected-dir
                (do
                  (generate-pdf data (.getCanonicalPath selected-dir))
                  (println "Form submitted successfully")
                  (swap! *state fx/swap-context assoc :errors {})
                  (component/show-dialog :information "Sucesso" "Contrato gerado com sucesso!"))
                (println "No directory selected")))))
        {:context context})
      (do
        (Platform/runLater
          (fn []
            (component/show-dialog :error "Erros no Formulário" "Corrija os errors no formulário")
            (println "Form has errors" validation-errors)))
        {:context (fx/swap-context context assoc :errors validation-errors)}))))

(defmethod event-handler ::type [{:keys [fx/context fx/event id]}]
  {:context (fx/swap-context context assoc-in [:data id] event)})

(defmethod event-handler ::blur [{:keys [fx/context id handler error-message]}]
  (let [value (fx/sub-val context get-in [:data id])
        errors (if (handler value)
                 (dissoc (:errors context) id)
                 (assoc (:errors context) id error-message))]
    {:context (fx/swap-context context assoc :errors errors)}))

;;; App
(def app
  (->> (Locale/setDefault (Locale/of "pt" "BR"))
       (fx/create-app *state
                      :event-handler event-handler
                      :desc-fn (fn [_] {:fx/type root-view}))))

;(fx/mount-renderer
;  *state
;  (do
;    (Locale/setDefault (Locale/of "pt" "BR"))
;    (fx/create-renderer
;      :middleware (fx/wrap-map-desc assoc :fx/type root-view)
;      :opts {:fx.opt/map-event-handler #(swap! *state (map-event-handler %))})))

;(defn -main []
;  (Locale/setDefault (Locale/of "pt" "BR"))
;  (fx/mount-renderer *state
;                     (fx/create-renderer
;                       :middleware (fx/wrap-map-desc assoc :fx/type root-view)
;                       :opts {:fx.opt/map-event-handler #(swap! *state (component/map-event-handler %))})))