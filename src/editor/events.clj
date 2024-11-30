(ns editor.events
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [editor.util :as util])
  (:use [clj-pdf.core])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)
           (java.util Locale)
           (javafx.application Platform)
           (javafx.scene Node)
           (javafx.scene.control Alert Alert$AlertType)
           (javafx.scene.input KeyCode KeyEvent MouseEvent)
           (javafx.stage DirectoryChooser)))

(defn format-date-with-capitalized-day [date]
  (let [formatted-date (str (.format (DateTimeFormatter/ofPattern "dd/MM/yyyy - EEEE") date))
        index (.indexOf formatted-date " - ")]
    (if (and (not= index -1) (< (+ index 3) (count formatted-date)))
      (str (subs formatted-date 0 (+ index 3))
           (clojure.string/capitalize (subs formatted-date (+ index 3) (+ index 4)))
           (subs formatted-date (+ index 4)))
      formatted-date)))

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
     [:paragraph [:chunk {:style :bold} "DATA DA FESTA: "] (format-date-with-capitalized-day (:date data))]
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
     [:paragraph "O valor total do presente contrato é de R$ " [:chunk {:style :bold} (util/format-currency (:total data))] " (por 3h de recreação)"]
     [:spacer]
     [:paragraph [:chunk {:style :bold} "CLÁUSULA QUARTA – "] [:chunk {:style :underline} "DAS CONDIÇÕES DE PAGAMENTO:"]]
     [:paragraph "A CONTRATANTE pagará a título de entrada o valor de R$ " (util/format-currency (:down_payment data)) " via " (:payment_method data) " e o restante no dia do evento."]
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

;; Utils
(defn show-dialog [type title text]
  (let [dialog (Alert. (case type
                         :information Alert$AlertType/INFORMATION
                         :warning Alert$AlertType/WARNING
                         :error Alert$AlertType/ERROR))]
    (.setTitle dialog title)
    (.setContentText dialog text)
    (.setHeaderText (.getDialogPane dialog) nil)
    (.showAndWait dialog)))

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
            (let [value (get data field)]
              (cond
                (empty? value) (assoc errors field "Campo obrigatório")
                (and (= field :cpf) (not (util/valid-cpf? value))) (assoc errors field "CPF inválido")
                :else errors)))
          {}
          required-fields))

(defmethod event-handler ::export-doc [{:keys [fx/context fx/event]}]
  (let [data (fx/sub-val context :data)
        required-fields [:name :cpf :kid :theme :address :time_start :time_end :num_kids :num_pros :min_age :max_age :total :down_payment]
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
                  {:context (fx/swap-context context assoc :errors {})}
                  (show-dialog :information "Sucesso" "Contrato gerado com sucesso!"))
                (println "No directory selected")))))
        {:context context})
      (do
        (Platform/runLater
          (fn []
            (show-dialog :error "Erros no Formulário" "Corrija os errors no formulário")
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