(ns ipp.parser-test
  (:require [clojure.test :refer :all]
            [ipp.parser :as parser]))

(deftest parse-malformed-request
  (is (= nil
         (parser/parse-ipp-request (map byte "malformed_request_body")))))

(defn readfile [fname]
  (map byte (slurp fname :encoding "ISO-8859-1")))

(def parsed-request {
  :version-major 2 :version-minor 0 :op 11 :request-id 1
  :groups [{
    :group :operation-attributes
    :attrs {
      "attributes-charset" [{:type :charset, :value "utf-8"}]
      "attributes-natural-language" [{:type :natural-language, :value "en-gb"}]
      "printer-uri" [{:type :uri, :value "http://localhost:6311/"}]
      "requested-attributes" [
          {:type :keyword, :value "compression-supported"}
          {:type :keyword, :value "copies-supported"}
          {:type :keyword, :value "cups-version"}
          {:type :keyword, :value "document-format-supported"}
          {:type :keyword, :value "marker-colors"}
          {:type :keyword, :value "marker-high-levels"}
          {:type :keyword, :value "marker-levels"}
          {:type :keyword, :value "marker-low-levels"}
          {:type :keyword, :value "marker-message"}
          {:type :keyword, :value "marker-names"}
          {:type :keyword, :value "marker-types"}
          {:type :keyword, :value "media-col-supported"}
          {:type :keyword, :value "multiple-document-handling-supported"}
          {:type :keyword, :value "operations-supported"}
          {:type :keyword, :value "print-color-mode-supported"}
          {:type :keyword, :value "printer-alert"}
          {:type :keyword, :value "printer-alert-description"}
          {:type :keyword, :value "printer-is-accepting-jobs"}
          {:type :keyword, :value "printer-state"}
          {:type :keyword, :value "printer-state-message"}
          {:type :keyword, :value "printer-state-reasons"}]}}]})

(deftest parse-request
  (is (= parsed-request
         (dissoc (parser/parse-ipp-request (readfile "test/ipp/files/request-01-get-printer-attributes.raw")) :body))))
