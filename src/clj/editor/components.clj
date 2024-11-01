(ns editor.components
  (:require [cljfx.api :as fx])
  (:import (java.util.function UnaryOperator)
           (javafx.scene.control Alert Alert$AlertType TextFormatter TextFormatter$Change)
           (javafx.util StringConverter)))

;; Formatters
(defn- format-cpf [digits]
  (let [digits (apply str (take 11 digits))                 ;; Limit to 11 digits
        part1 (subs digits 0 (min 3 (count digits)))
        part2 (if (> (count digits) 3) (subs digits 3 (min 6 (count digits))) "")
        part3 (if (> (count digits) 6) (subs digits 6 (min 9 (count digits))) "")
        part4 (if (> (count digits) 9) (subs digits 9 (min 11 (count digits))) "")]
    (str (when (seq part1) part1)
         (when (seq part2) (str "." part2))
         (when (seq part3) (str "." part3))
         (when (seq part4) (str "-" part4)))))

(def cpf-value-converter
  (proxy [StringConverter] []
    (fromString [text] text)
    (toString [text]
      (when (> (count text) 0)
        (format-cpf (re-seq #"\d" text))))))

(def cpf-value-filter
  (proxy [UnaryOperator] []
    (apply [change]
      (let [new-text (.getControlNewText change)]
        (if (re-matches #"\d{0,3}\.?\d{0,3}\.?\d{0,3}-?\d{0,2}" new-text) ;; should match the CPF pattern
          change
          nil)))))

(defn- format-time [digits]
  (let [digits (apply str (take 4 digits))                  ;; Limit to 4 digits
        part1 (subs digits 0 (min 2 (count digits)))
        part2 (if (> (count digits) 2) (subs digits 2 (min 4 (count digits))) "")]
    (str (when (seq part1) part1)
         (when (seq part2) (str ":" part2)))))

(def time-value-converter
  (proxy [StringConverter] []
    (fromString [text] text)
    (toString [text]
      (when (> (count text) 0)
        (format-time (re-seq #"\d" text))))))

(def time-value-filter
  (proxy [UnaryOperator] []
    (apply [change]
      (let [new-text (.getControlNewText change)]
        (if (re-matches #"\d{0,2}:?\d{0,2}" new-text)       ;; should match the HH:MM pattern
          change
          nil)))))

(defn numeric-formatter [max-length]
  (TextFormatter.
    (reify UnaryOperator
      (apply [_ change]
        (let [new-text (.getControlNewText ^TextFormatter$Change change)]
          (if (and (re-matches #"\d*" new-text)
                   (<= (count new-text) max-length))
            change
            nil))))))

(defn- format-money [digits]
  (str (apply str (take 15 digits)) ",00"))

(def currency-value-converter
  (proxy [StringConverter] []
    (fromString [text] text)
    (toString [text]
      (if (and (seq text) (not (clojure.string/includes? text ",")))
        (format-money (re-seq #"\d" text))
        text))))

(def currency-value-filter
  (proxy [UnaryOperator] []
    (apply [change]
      (let [new-text (.getControlNewText change)]
        (if (re-matches #"\d{0,15},?\d{0,2}" new-text)
          change
          nil)))))

(defn currency-text-formatter []
  (TextFormatter.
    (proxy [StringConverter] []
      (fromString [text] text)
      (toString [text]
        (if (and (seq text) (not (clojure.string/includes? text ",")))
          (format-money (re-seq #"\d" text))
          text)))
    nil
    (proxy [UnaryOperator] []
      (apply [change]
        (let [new-text (.getControlNewText change)]
          (if (re-matches #"\d{0,15},?\d{0,2}" new-text)
            change
            nil))))))

;; Validators
(defn valid-cpf? [cpf]
  (if (empty? cpf)
    true
    (let [digits (map #(Character/digit ^char (first %) 10) (re-seq #"\d" cpf))]
      (and (= 11 (count digits))
           (not (apply = digits))
           (let [dv1 (mod (reduce + (map * (range 10 1 -1) (take 9 digits))) 11)
                 dv1 (if (< dv1 2) 0 (- 11 dv1))
                 dv2 (mod (reduce + (map * (range 11 1 -1) (take 10 digits))) 11)
                 dv2 (if (< dv2 2) 0 (- 11 dv2))]
             (and (= dv1 (nth digits 9))
                  (= dv2 (nth digits 10))))))))

(defn calculate-percentage [down-payment total]
  (if (or (empty? down-payment) (empty? total) (zero? (read-string total)))
    "(0%)"
    (str "(" (int (* 100 (/ (read-string down-payment) (read-string total)))) "%)")))

;; Components
(defn tab
  [{:keys [label content]}]
  {:fx/type  :tab
   :closable false
   :text     label
   :content  content})

(defn show-dialog [type title text]
  (let [dialog (Alert. (case type
                         :information Alert$AlertType/INFORMATION
                         :warning Alert$AlertType/WARNING
                         :error Alert$AlertType/ERROR))]
    (.setTitle dialog title)
    (.setContentText dialog text)
    (.setHeaderText (.getDialogPane dialog) nil)
    (.showAndWait dialog)))

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

(defn labeled-input
  [{:keys [fx/context label id max-width prompt-text text-formatter on-focused-changed style]}]
  (let [errors (fx/sub-val context :errors)
        text (or (fx/sub-val context get-in [:data id]) "")]
    {:fx/type    :v-box
     :fill-width true
     :children   (cond-> [{:fx/type :label
                           :text    (or label "")}
                          (cond-> {:fx/type         :text-field
                                   :text            text
                                   :prompt-text     (or prompt-text "")
                                   :max-width       (or max-width Double/MAX_VALUE)
                                   :on-text-changed {:event/type :editor.core/type :id id}}
                                  style (assoc :style style)
                                  text-formatter (assoc :text-formatter text-formatter)
                                  on-focused-changed (assoc :on-focused-changed on-focused-changed)
                                  (contains? errors id) (update :style merge {:-fx-border-color "red"}))]
                         ;; Add error label if there's an error
                         (contains? errors id) (conj {:fx/type :label
                                                      :text    (get errors id)
                                                      :style   {:-fx-text-fill "red"}}))}))

(defn labeled-date-picker
  [{:keys [label id value]}]
  {:fx/type    :v-box
   :fill-width true
   :children   [{:fx/type :label
                 :text    label}
                {:fx/type          :date-picker
                 :editable         false
                 :on-value-changed {:event/type :editor.core/type :id id}
                 :value            value}]})

(defn form-row
  [{:keys [children]}]
  {:fx/type  :h-box
   :spacing  20
   :padding  {:bottom 10}
   :children children})