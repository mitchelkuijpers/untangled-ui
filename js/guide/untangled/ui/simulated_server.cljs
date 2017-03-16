(ns untangled.ui.simulated-server
  (:require
    [om.next :as om]
    [untangled.client.impl.network :as u-net]))

(defrecord MockNetwork [server]
  u-net/UntangledNetwork
  (send [this edn ok err]
    (let [resp (server {} edn)]
      (js/setTimeout #(ok resp) 700)))
  (start [this app] this))

(defn make-mock-network [state read+mutate]
  (->MockNetwork
    (let [parser (om/parser read+mutate)]
      (fn [env tx] (parser (assoc env :state state) tx)))))
