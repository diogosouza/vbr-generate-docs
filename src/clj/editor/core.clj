(ns editor.core
  (:gen-class)
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [editor.components :as component]
            [editor.events :as events]
            [editor.util :as util])
  (:use [clj-pdf.core])
  (:import (java.time LocalDate)
           (java.util Locale)
           (javafx.scene.control ToggleGroup)))

(defn get-version [& _]
  (-> "project.clj" slurp read-string (nth 2)))

; Global state
(def *state
  (atom (fx/create-context
          {:data          {:name           ""
                           :cpf            ""
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
                             :on-focused-changed {:event/type    ::events/blur
                                                  :id            :cpf
                                                  :handler       util/valid-cpf?
                                                  :error-message "CPF inválido"}
                             :text-formatter     {:fx/type         :text-formatter
                                                  :value-converter util/cpf-value-converter
                                                  :filter          util/cpf-value-filter
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
                                           :children [{:fx/type        component/text-input
                                                       :id             :time_start
                                                       :max-width      60
                                                       :text           "18:00"
                                                       :text-formatter {:fx/type         :text-formatter
                                                                        :value-converter util/time-value-converter
                                                                        :filter          util/time-value-filter
                                                                        :value           (:time_start data)}}
                                                      {:fx/type   :label
                                                       :text      "às"
                                                       :padding   {:top 8}
                                                       :alignment :bottom-center}
                                                      {:fx/type        component/text-input
                                                       :id             :time_end
                                                       :max-width      60
                                                       :text           "21:00"
                                                       :text-formatter {:fx/type         :text-formatter
                                                                        :value-converter util/time-value-converter
                                                                        :filter          util/time-value-filter
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
       :on-key-pressed {:event/type ::events/key-pressed :edit-prop edit-prop}
       :text-formatter {:fx/type          :text-formatter
                        :value-converter  value-converter
                        :value            value
                        :on-value-changed {:event/type ::events/edit-description
                                           :prop       prop
                                           :edit-prop  edit-prop
                                           :attr       attr
                                           :id         id}}}
      {:fx/type          :label
       :on-mouse-clicked {:event/type ::events/on-cell-click :edit-prop edit-prop :id id :attr attr}
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
     :on-selected-changed {:event/type ::events/toggle-selected :prop prop :id id}}))

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
                           :text-formatter (util/numeric-formatter 4)}

                          {:fx/type    :v-box
                           :fill-width true
                           :children   [{:fx/type :label
                                         :text    "Idade Média:"}
                                        {:fx/type  :h-box
                                         :spacing  10
                                         :children [{:fx/type        component/text-input
                                                     :id             :min_age
                                                     :max-width      60
                                                     :prompt-text    "Min"
                                                     :text-formatter (util/numeric-formatter 2)}
                                                    {:fx/type   :label
                                                     :text      "a"
                                                     :padding   {:top 8}
                                                     :alignment :bottom-center}
                                                    {:fx/type        component/text-input
                                                     :id             :max_age
                                                     :max-width      60
                                                     :prompt-text    "Max"
                                                     :text-formatter (util/numeric-formatter 2)}]}]}

                          {:fx/type        component/labeled-input
                           :label          "N. Profissionais:"
                           :id             :num_pros
                           :max-width      80
                           :h-box/hgrow    :always
                           :text-formatter (util/numeric-formatter 2)}]}

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
                                          :on-text-changed {:event/type ::events/set-extra
                                                            :extra-prop :extra-activity}
                                          :on-key-pressed  {:event/type ::events/add-property-enter
                                                            :prop       :activities
                                                            :attr       :activity
                                                            :extra-prop :extra-activity}
                                          :on-add-action   {:event/type ::events/add-property-action
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
                                          :on-text-changed {:event/type ::events/set-extra
                                                            :extra-prop :extra-workshop}
                                          :on-key-pressed  {:event/type ::events/add-property-enter
                                                            :prop       :workshops
                                                            :attr       :workshop
                                                            :extra-prop :extra-workshop}
                                          :on-add-action   {:event/type ::events/add-property-action
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
                                       :on-text-changed {:event/type ::events/type :id :extra_items}
                                       :h-box/hgrow     :always}]}]}]})

(defn values-section [{:keys [fx/context]}]
  (let [total (fx/sub-val context get-in [:data :total])
        down_payment (fx/sub-val context get-in [:data :down_payment])]
    {:fx/type   :v-box
     :spacing   10
     :alignment :bottom-right
     :children  [{:fx/type  component/form-row
                  :children [{:fx/type  :v-box
                              :children [{:fx/type :label
                                          :text    "Forma de Pagamento:"}
                                         {:fx/type          :combo-box
                                          :min-width        300
                                          :on-value-changed {:event/type ::events/type :id :payment_method}
                                          :items            ["PIX", "Cartão de Crédito", "Débito", "Dinheiro"]
                                          :value            "PIX"}]}]}

                 {:fx/type  component/form-row
                  :children [{:fx/type        component/labeled-input
                              :label          "Total:"
                              :id             :total
                              :h-box/hgrow    :always
                              :style          {:-fx-font-size   14
                                               :-fx-font-weight "bold"
                                               :-fx-padding     "8 14"}
                              :text-formatter {:fx/type         :text-formatter
                                               :value-converter :default
                                               :filter          util/currency-value-filter
                                               :value           total}}
                             {:fx/type        component/labeled-input
                              :label          "Entrada:"
                              :id             :down_payment
                              :h-box/hgrow    :always
                              :style          {:-fx-font-size   14
                                               :-fx-font-weight "bold"
                                               :-fx-padding     "8 14"}
                              :text-formatter {:fx/type         :text-formatter
                                               :value-converter :default
                                               :filter          util/currency-value-filter
                                               :value           down_payment}}]}

                 {:fx/type :label
                  :id      "down_payment_percentage"
                  :text    (util/calculate-percentage down_payment total)}]}))

(defn export-pdf-button [_]
  {:fx/type   :v-box
   :alignment :center
   :children  [{:fx/type   :button
                :text      "Exportar PDF"
                :style     {:-fx-font-weight "bold"
                            :-fx-padding     "10 20"}
                :on-action {:event/type ::events/export-doc}}]})

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

;; App
(defn -main [& _]
  (Locale/setDefault (Locale/forLanguageTag "pt-BR"))
  (fx/create-app *state
                 :event-handler events/event-handler
                 :desc-fn (fn [_] {:fx/type root-view})))