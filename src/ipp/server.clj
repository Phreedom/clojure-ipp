(ns ipp.server
  (:use [ipp.enums])
  (:require
    [ipp.protocol :as protocol]
    [ipp.parser :as parser]
    [ipp.serializer :as serializer]
    [ipp.operations :as operations]

    [immutant.web :as http-server]
    [byte-streams :as bs]
    [clojure.string :as string]
    [clojure.tools.logging :as log]))

(defn ^:private human-detected!!! [{:keys [uri]} request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (str "This is a virtual IPP printer. "
              "Use this URL to print: " uri)})

(defn ^:private ipp-request? [{:keys [queue]} request]
  (and (= (:request-method request) :post)
       (= (:uri request) (str "/" queue))
       (= (get-in request [:headers "content-type"]) "application/ipp")))

(defn ^:private construct-http-response [ipp-response]
  (let [resp (serializer/ipp-response ipp-response)]
    (log/trace (with-out-str (bs/print-bytes (bs/to-byte-buffer (byte-array (map unchecked-byte resp))))))
    {:status 200
    :headers {"content-type" "application/ipp"}
    :body (bs/to-input-stream (byte-array (map unchecked-byte resp)))}))

(def ^:private malformed-request {
  :status 400
  :headers {"content-type" "text/html"}
  :body "Failed to parse IPP request"})

(defn ^:private handle-ipp-request [config request]
  (let [body (bs/to-byte-array (:body request))];FIXME: use a stream instead?
    (let [req (parser/parse-ipp-request body)]
      (log/debug "Got an IPP request " (dissoc req :body))
      (log/trace (with-out-str (bs/print-bytes (byte-array (take 300 body)))))
      (if req
        (let [resp (operations/perform-operation config {} req)]
          (log/debug "Responding with " resp)
          (construct-http-response resp))
        malformed-request))))

(defn ^:private ipp-handler [config request]
  (if (ipp-request? config request)
      (handle-ipp-request config request)
      (human-detected!!! config request)))

(defn start-server [{:keys [host port] :as config}]
  (assoc config :server-instance
                (http-server/run #(ipp-handler config %)
                                 :host host :port port)))

(defn stop-server [config]
  (http-server/stop (:server-instance config))
  (dissoc config :server-instance))

(defn ^:private non-empty-string [s]
  (and (string? s) (not (string/blank? s))))

(defn make-server [{:keys [host port handler queue name] :as config}]
  {:pre [(non-empty-string host)
         (non-empty-string queue)
         (non-empty-string name)
         (< 0 port 65535)
         (protocol/handler? handler)]}
  (assoc config :uri (str "ipp://" host ":" port "/" queue)))
