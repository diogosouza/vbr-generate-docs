(ns editor.util
  (:require [clojure.string :as str])
  (:import (java.util.function UnaryOperator)
           (javafx.scene.control TextFormatter TextFormatter$Change)
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

(defn- format-time [digits]
  (let [digits (apply str (take 4 digits))                  ;; Limit to 4 digits
        part1 (subs digits 0 (min 2 (count digits)))
        part2 (if (> (count digits) 2) (subs digits 2 (min 4 (count digits))) "")]
    (str (when (seq part1) part1)
         (when (seq part2) (str ":" part2)))))

(defn numeric-formatter [max-length]
  (TextFormatter.
    (reify UnaryOperator
      (apply [_ change]
        (let [new-text (.getControlNewText ^TextFormatter$Change change)]
          (if (and (re-matches #"\d*" new-text)
                   (<= (count new-text) max-length))
            change
            nil))))))

(defn format-currency [total]
  (let [number (Double/parseDouble (str/replace total "," "."))]
    (format "%.2f" number)))

;; Converters
(def cpf-value-converter
  (proxy [StringConverter] []
    (fromString [text] text)
    (toString [text]
      (when (> (count text) 0)
        (format-cpf (re-seq #"\d" text))))))

(def time-value-converter
  (proxy [StringConverter] []
    (fromString [text] text)
    (toString [text]
      (when (> (count text) 0)
        (format-time (re-seq #"\d" text))))))

;; Filters
(def cpf-value-filter
  (proxy [UnaryOperator] []
    (apply [change]
      (let [new-text (.getControlNewText change)]
        (if (re-matches #"\d{0,3}\.?\d{0,3}\.?\d{0,3}-?\d{0,2}" new-text) ;; should match the CPF pattern
          change
          nil)))))

(def time-value-filter
  (proxy [UnaryOperator] []
    (apply [change]
      (let [new-text (.getControlNewText change)]
        (if (re-matches #"\d{0,2}:?\d{0,2}" new-text)       ;; should match the HH:MM pattern
          change
          nil)))))

(def currency-value-filter
  (proxy [UnaryOperator] []
    (apply [change]
      (let [new-text (.getControlNewText change)]
        (if (re-matches #"\d{0,15},?\d{0,2}" new-text)
          change
          nil)))))

;; Utils
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