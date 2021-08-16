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


(defn- apply-rate-to-item [item rate short-rate]
  (let [{:keys [amount memo
                subtransactions]} item
        can-update? (not (seq subtransactions))
        new-amount                (int (* rate amount))
        new-memo                  (->> [place-holder
                                        (double (/ amount 1000))
                                        (y.config/source-currency)
                                        (if can-update?
                                          short-rate
                                          (str "❗" rate))
                                        (string/trim memo)]
                                       (string/join "|" ))

        short-memo (subs new-memo 0 (min (count new-memo) 200))]
    (cond-> (assoc item :memo short-memo)
      can-update? (assoc :amount new-amount))))


(defn- apply-rate [lookup transaction]
  (let [{:keys [rate short-rate]} (get lookup (:date transaction))
        {:keys [amount]} transaction]
    (-> transaction
        (apply-rate-to-item rate short-rate)
        (assoc :old-amount amount))))


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
  (let [changes (get-updated-transactions from-date)]
    (if (seq changes)
      (print-changed-transactions changes)
      (prn "No changes"))))


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
