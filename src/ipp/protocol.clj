(ns ipp.protocol)

(defprotocol IPrintJobHandler
  (accept-job [_ queue job]
    "Called after a complete job was received. This is the main entry
    point."))


(defn handler?
  "Checks if H is a valid handler (e.g. at minimum implementes
  IPrintJobHandler)"
  [h]
  (satisfies? IPrintJobHandler h))
