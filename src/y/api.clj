(ns api
  (:require [y.config :as config]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))



(defn- wrap-token [request token]
  (assoc-in request [:headers "Authorization"] (str "Bearer " token)))


(defn- wrap-endpoint [request]
  (-> request
      (update :url #(str "https://api.youneedabudget.com/v1" %))
      (assoc :insecure? false)))


(defn- wrap-json [request]
  (let [has-body?     (contains? request :body)
        body-request? (contains? #{:post :put} (:method request))]
    (cond-> request
      true                          (assoc-in [:headers "Content-Type"] "application/json; charset=utf-8")
      (and body-request? has-body?) (update :body json/generate-string))))


(defn- parse-response [response]
  (if (= 200 (:status response))
    (cond-> response
      (string? (:body response))
      (update :body json/parse-string keyword)
      true
      (get-in [:body :data]))
    (throw (ex-info "Request Error" response))))


(defn- client
  [request]
  (-> request
      (wrap-token (config/token))
      wrap-endpoint
      wrap-json
      http/request
      deref
      parse-response))


(defn get-budgets []
  (client
    {:method :get :url "/budgets"}))


(defn get-accounts
  []
  (client
    {:method :get :url (str "/budgets/" (config/budget-id) "/accounts")}))


(defn get-transactions
  "since-date '2021-07-25' "
  [account-id since-date]
  (client
    {:method :get
     :url    (str "/budgets/" (config/budget-id) "/accounts/" account-id "/transactions?since_date=" since-date)}))
