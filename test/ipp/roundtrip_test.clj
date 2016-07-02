(ns ipp.roundtrip-test
  (:require [clojure.test :refer :all]
            [ipp.parser :as parser]
            [ipp.serializer :as serializer]))

(def printer-attributes-response {
   :version_major 2 :version_minor 0 :op 0 :request_id 10
   :groups [
     {:group :printer-attributes :attrs {
       "ipp-versions-supported" [{:type :keyword :value "2.0"}]
       "printer-state" [{:type :enum :value 3}]
       "document-format-default" [{:type :mime-media-type :value "application/pdf"}]
       "uri-security-supported" [{:type :keyword :value "none"}]
       "printer-is-accepting-jobs" [{:type :boolean :value true}]
       "generated-natural-language-supported" [{:type :natural-language :value "en"}]
       "document-format-supported" [{:type :mime-media-type, :value "application/pdf"}
                                     {:type :mime-media-type, :value "application/postscript"}
                                     {:type :mime-media-type, :value "application/octet-stream"}]
       "charset-configured" [{:type :charset, :value "utf-8"}]
       "printer-state-reasons" [{:type :keyword, :value "none"}]
       "queued-job-count" [{:type :integer, :value 0xabc}]
       "printer-location" [{:type :text-without-language, :value "Virtual"}]
       "natural-language-configured" [{:type :natural-language, :value "en"}]
       "printer-up-time" [{:type :integer, :value 1}]
       "printer-name" [{:type :name-without-language, :value "Pepa DMS IPP Printer"}]
       "charset-supported" [{:type :charset, :value "utf-8"}]
       "operations-supported" [{:type :enum, :value 2} {:type :enum, :value 4} {:type :enum, :value 8} {:type :enum, :value 9} {:type :enum, :value 10} {:type :enum, :value 11}]
       "printer-uri-supported" [{:type :uri, :value "ipp://localhost:6311/documents"}]
       "uri-authentication-supported" [{:type :keyword, :value "none"}]
       "pdl-override-supported" [{:type :keyword, :value "not-attempted"}]
       "compression-supported" [{:type :keyword, :value "none"}]}}
     {:group :operation-attributes :attrs {
       "attributes-charset" [{:type :charset, :value "utf-8"}]
       "attributes-natural-language" [{:type :natural-language, :value "en"}]}}]})

(deftest roundtrip-printer-attributes-response
  (is (= printer-attributes-response
         (dissoc (parser/parse-ipp-request (serializer/serialize-ipp-response printer-attributes-response)) :body))))

(def quirks-sample {
   :version_major 2 :version_minor 0 :op 0 :request_id 10
   :groups [
     {:group :printer-attributes :attrs {
       "printer-is-accepting-jobs" [{:type :integer :value 111111}]
       "reserved-type-value" [{:type :reserved-20 :value "some unparsable blob"}]
       "reserved-type-value2" [{:type :reserved-95 :value "another unparsable blob"}]
       "ipp-versions-supported" [{:type :keyword :value "2.0"}]}}
     {:group :job-attributes :attrs{}}
     {:group :job-attributes :attrs{}}]})

(deftest roundtrip-quirks-response
  (is (= quirks-sample
         (dissoc (parser/parse-ipp-request (serializer/serialize-ipp-response quirks-sample)) :body))))


(defn readfile [fname]
  (map byte (slurp fname :encoding "ISO-8859-1")))

(defn file-roundtrip-test [fname]
  (let [body (readfile (str "test/ipp/files/" fname))]
    (is (= body (serializer/serialize-ipp-response (parser/parse-ipp-request body))))))
    
(deftest rountrip-1
  (file-roundtrip-test "request-01-get-printer-attributes.raw"))

(deftest rountrip-2
  (file-roundtrip-test "request-02-validate-job.raw"))

(deftest rountrip-3
  (file-roundtrip-test "request-03-get-printer-attributes.raw"))

(deftest rountrip-4
  (file-roundtrip-test "request-04-print-job.raw"))

(deftest rountrip-5
  (file-roundtrip-test "request-05-get-jobs.raw"))

(deftest rountrip-6
  (file-roundtrip-test "request-06-get-printer-attributes.raw"))

(deftest rountrip-7
  (file-roundtrip-test "request-07-get-printer-attributes.raw"))

