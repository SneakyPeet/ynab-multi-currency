# What

Convert the currency for transactions in ynab accounts.

* Will convert the currency in your selected accounts for all transactions since a given date based on the exchange rate for the transaction date.
* Will update the memo to include the currency conversion information

# Setup

* Install babashka
* Update config.edn with
  * ynab token
  * fixerio token
  * source and destination currencies
* `bb run budgets` and set the relevant `budget-id` in config.edn
* `bb run accounts` and set the account-id's you want to update in config.edn

# Usage

* View changes that will be applied: `bb run view <from-date>`. example: `bb run view 2021-07-15`
* Apply changes: `bb run apply <from-date>`. example: `bb run apply 2021-07-15`

# Dev

`bb --nrepl-server`

`C-c C-x c j (cider-connect-clj)`
