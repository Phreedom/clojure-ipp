(ns ipp.test-instance
  (:use [ipp.server])
  (:require [ipp.protocol :as protocol]
            [byte-streams :as bs]
            [clojure.tools.logging :as log]))

(def my-handler
  (reify
    protocol/IPrintJobHandler
    (accept-job [_ queue job]
      (log/info "got job on queue" queue)
      (log/info (dissoc job :data))
      (bs/print-bytes (bs/to-byte-buffer (byte-array (take 256 (:data job))))))))

(defonce my-server (make-server {
  :host "localhost"
  :port 6311
  :handler my-handler
  :queue "documents"
  :name "Pepa DMS IPP Printer"}))

(defn start []
  (alter-var-root #'my-server start-server))

(defn stop []
  (alter-var-root #'my-server stop-server))
