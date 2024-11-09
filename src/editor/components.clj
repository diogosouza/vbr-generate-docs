(ns editor.components
  (:require [cljfx.api :as fx]
            [editor.events :as events]))

(defn tab
  [{:keys [label content]}]
  {:fx/type  :tab
   :closable false
   :text     label
   :content  content})

(defn form-row
  [{:keys [children]}]
  {:fx/type  :h-box
   :spacing  20
   :padding  {:bottom 10}
   :children children})

(defn section-divider [{:keys [text]}]
  {:fx/type  :v-box
   :spacing  5
   :padding  {:top 20 :bottom 20}
   :children [{:fx/type        :label
               :text           text
               :style          {:-fx-font-size   14
                                :-fx-font-weight "bold"}
               :alignment      :center
               :max-width      Double/MAX_VALUE             ;; Ensure the label stretches
               :text-alignment :center}
              {:fx/type :separator}                         ;; Full-width separator
              ]})

(defn text-input
  [{:keys [fx/context id max-width prompt-text text-formatter on-focused-changed style label]}]
  (let [errors (fx/sub-val context :errors)
        text (or (fx/sub-val context get-in [:data id]) "")]
    {:fx/type    :v-box
     :fill-width true
     :children   (cond-> []
                         label (conj {:fx/type :label
                                      :text    (or label "")})
                         true (conj (cond-> {:fx/type         :text-field
                                             :text            text
                                             :prompt-text     (or prompt-text "")
                                             :max-width       (or max-width Double/MAX_VALUE)
                                             :on-text-changed {:event/type ::events/type :id id}}
                                            style (assoc :style style)
                                            text-formatter (assoc :text-formatter text-formatter)
                                            on-focused-changed (assoc :on-focused-changed on-focused-changed)
                                            (contains? errors id) (update :style merge {:-fx-border-color "red"})))
                         (contains? errors id) (conj {:fx/type :label
                                                      :text    (get errors id)
                                                      :style   {:-fx-text-fill "red"}}))}))

(defn labeled-input
  [params]
  (text-input params))

(defn labeled-date-picker
  [{:keys [label id value]}]
  {:fx/type    :v-box
   :fill-width true
   :children   [{:fx/type :label
                 :text    label}
                {:fx/type          :date-picker
                 :editable         false
                 :on-value-changed {:event/type ::events/type :id id}
                 :value            value}]})