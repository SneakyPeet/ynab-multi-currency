(ns y.core
  (:require [y.config :as config]
            [y.api :as api]
            [clojure.string :as string]
            [clojure.pprint :as pp]))


(def ^:private place-holder (config/prefix))

(defn- should-convert? [transaction]
  (not (string/starts-with? (:memo transaction) place-holder)))


(defn- get-transactions-to-convert [from-date]
  (->> (config/account-ids)
       (map (fn [id] (api/get-transactions id from-date)))
       (reduce into)
       (filter should-convert?)))


(defn- date-rate-lookup [transactions]
  (->> transactions
       (map :date)
       set
       (map (fn [d]
              (let [rate (y.api/get-currency-rate-for-date d)]
                [d {:rate rate
                    :short-rate (-> rate (* 10000) int (/ 10000) double)}])) )
       (into {})))


(defn- apply-rate [lookup transaction]
  (let [{:keys [rate short-rate]} (get lookup (:date transaction))
        {:keys [amount memo]} transaction
        new-amount (int (* rate amount))
        new-memo (string/join "|" [place-holder
                                   (double (/ amount 1000))
                                   (y.config/source-currency)
                                   short-rate
                                   (string/trim memo)])

        short-memo (subs new-memo 0 (min (count new-memo) 200))]
    (assoc transaction
           :old-amount amount
           :amount new-amount
           :memo short-memo)))


(defn- get-updated-transactions [from-date]
  (let [transactions (get-transactions-to-convert from-date)
        rate-lookup  (date-rate-lookup transactions)]
    (map #(apply-rate rate-lookup %)transactions)))


(defn- print-changed-transactions [transactions]
  (->> transactions
       (map (fn [t]
              (-> t
                  (assoc :to "->")
                  (update :amount #(double (/ % 1000)))
                  (update :old-amount #(double (/ % 1000))))))
       (pp/print-table [:date :old-amount :to :amount :memo])))


(defn print-changes [from-date]
  (->> (get-updated-transactions from-date)
       print-changed-transactions))


(defn apply-changes [from-date]
  (prn "Fetching Transactions")
  (let [transactions (get-updated-transactions from-date)]
    (if (empty? transactions)
      (prn "Nothing to update")
      (do
        (print-changed-transactions transactions)
        (prn "Saving")
        (api/save-transactions (map #(dissoc % :old-amount) transactions))
        (prn "Done")))))
