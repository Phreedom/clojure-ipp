(ns ipp.operations
  (:use [ipp.enums])
  (:require [byte-streams :as bs]
            [ipp.protocol :as protocol]))

(def supported-operations (vals operation-to-tag))

(defn value [type val] {
  :type type :value val})

(defn get-attr-value [req group name]
  (let [group-map (first (filter #(= group (:group %)) (:groups req)))
        attr (when group-map (get-in group-map [:attrs name]))]
    (when attr (:value (first attr)))))

(defn ^:private construct-printer-attributes-group [{:keys [uri name]}]
  {:group :printer-attributes :attrs {
    "printer-uri-supported" [(value :uri uri)]
    "uri-security-supported" [(value :keyword "none")]
    "uri-authentication-supported" [(value :keyword "none")]
    "printer-name" [(value :name-without-language name)]
    "printer-location" [(value :text-without-language "Virtual")]
    "printer-state" [(value :enum (:idle printer-state))]
    "printer-state-reasons" [(value :keyword "none")]
    "ipp-versions-supported" [(value :keyword "2.0")]

    "operations-supported" (map #(value :enum %) supported-operations)

    "charset-configured" [(value :charset "utf-8")]
    "charset-supported" [(value :charset "utf-8")]
    "natural-language-configured" [(value :natural-language "en")]
    "generated-natural-language-supported" [(value :natural-language "en")]

    "document-format-default" [(value :mime-media-type "application/pdf")]
    "document-format-supported" (map #(value :mime-media-type %)
                                     ["application/pdf" "application/postscript" "application/octet-stream"])
    "printer-is-accepting-jobs" [(value :boolean true)]
    "queued-job-count" [(value :integer 0)]
    "pdl-override-supported" [(value :keyword "not-attempted")]
    "printer-up-time" [(value :integer 1)]
    "compression-supported" [(value :keyword "none")]

    ;required output-bin extension according to PWG5100.2
    "output-bin-default" [(value :keyword "top")]
    "output-bin-supported" [(value :keyword "top")]

    "copies-default" [(value :integer 1)]
    "color-supported" [(value :boolean true)]
    "sides-default" [(value :keyword "one-sided")]
    "sides-supported" [(value :keyword "one-sided")]
  }})

(defn ^:private construct-operation-attributes-group [status]
  {:group :operation-attributes :attrs {
    "attributes-charset" [(value :charset "utf-8")]
    "attributes-natural-language" [(value :natural-language "en")] }})

(defn ^:private construct-ipp-response [{:keys [request_id]} status groups]
  {:version_major 2
   :version_minor 0
   :op (get status-to-code status)
   :request_id request_id
   :groups (concat [(construct-operation-attributes-group status)] groups)})

(defmulti perform-operation (fn [config state req]
                                (get tag-to-operation (:op req))))

(defmethod perform-operation :default [_ _ req]
  (construct-ipp-response req :server-error-operation-not-supported []))

(defmethod perform-operation :get-printer-attributes [config _ req]
  (let [printer (construct-printer-attributes-group config)]
    (construct-ipp-response req :successful [printer])))

; If we can parse the request, we consider it validated
; since we only really care about the PDF file.
(defmethod perform-operation :validate-job [_ _ req]
  (construct-ipp-response req :successful []))

; if we receive a cancel request, we assume the client
; knows what it's doing and just say OK
(defmethod perform-operation :cancel-job [_ _ req]
  (construct-ipp-response req :successful []))

; According to rfc3196 3.1.2.3.9: we aren't required to keep completed jobs
; and all jobs finish as soon as print-job response is sent.
; So we always return an empty list.
(defmethod perform-operation :get-jobs [_ _ req]
  (construct-ipp-response req :successful []))

(defn ^:private job-id-to-uri [config id]
  (str (:uri config) "/job" id))

(defn ^:private uri-to-job-id [uri]
  (let [job-str (last (re-find (re-pattern "/job(\\d+)$") nil))]
    (when job-str (Integer. job-str))))

; We grab the PDF file, send it to the handler and report the job as complete.
; CUPS doesn't believe it, and tends to follow up with get-jobs and get-job-attributes to confirm.
(defmethod perform-operation :print-job [{:keys [queue handler] :as config} _ req]
  (assert (protocol/handler? handler))
  (protocol/accept-job handler queue {
    :job-name (get-attr-value req :operation-attributes "job-name")
    :document-name (get-attr-value req :operation-attributes "document-name")
    :data (:body req)})
  ; make job-id and job-uri follow the same pattern so that we have less state to manage
  (let [job-id (+ 1 (rand-int 0xffffff))]
    (construct-ipp-response req :successful [{:group :job-attributes :attrs {
      "job-uri" [(value :uri (job-id-to-uri config job-id))]
      "job-id" [(value :integer job-id)]
      "job-state" [(value :enum (:completed job-state))]
    }}])))

; Report all jobs as completed because we finish processing the job
; during the print-job request.
; According to rfc3196 3.1.2.3.9: we could also reply with
; "client-error-not-found" or "client-error-gone". Maybe this
; would be a better reply considering our implementation of get-jobs.
(defmethod perform-operation :get-job-attributes [config _ req]
  (let [job-uri-attr (get-attr-value req :operation-attributes "job-uri")
        job-uri-val (when job-uri-attr (uri-to-job-id job-uri-attr))
        job-id-val (get-attr-value req :operation-attributes "job-id")
        job-id (or job-id-val job-uri-val)]
    (if job-id
      (construct-ipp-response req :successful [{:group :job-attributes :attrs {
        "job-uri" [(value :uri (job-id-to-uri config job-id))]
        "job-id" [(value :integer job-id)]
        "job-state" [(value :enum (:completed job-state))]}}])
      (construct-ipp-response req :client-error-bad-request []))))
