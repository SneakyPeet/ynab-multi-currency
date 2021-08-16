(ns y.config
  (:require [clojure.edn]))


(def ^:private c (clojure.edn/read-string (slurp "config.edn")))


(defn token [] (:token c))

(defn budget-id [] (:budget-id c))

(defn account-ids [] (:account-ids c))

(defn fixer-token [] (:fixer-io-token c))

(defn source-currency [] (:source-currency c))

(defn destination-currency [] (:destination-currency c))

(defn prefix [] (:prefix c))
