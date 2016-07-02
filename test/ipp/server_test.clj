(ns ipp.server-test
  (:require [clojure.test :refer :all]
            [ipp.server :as server]))

(def config {
  :host "localhost"
  :port 6311
  :queue "documents"
  :uri "ipp://localhost:6311/documents"
  :name "Pepa DMS IPP Printer"})

(deftest human-friendly
  (is (= (get-in (#'server/ipp-handler config {}) [:headers "content-type"]) "text/html")))


(defn ipp-req [body] {
  :request-method :post
  :uri "/documents"
  :headers {"content-type" "application/ipp"}
  :body (byte-array body)})

(deftest malformed-request
  (let [malformed-req (ipp-req (map byte "malformedbody"))]
    (is (= (:status (#'server/ipp-handler config malformed-req)) 400))))


(defn readfile [fname]
  (map byte (slurp (str "test/ipp/files/" fname) :encoding "ISO-8859-1")))

(deftest get-printer-attributes-request
  (let [req (ipp-req (readfile "request-01-get-printer-attributes.raw"))]
    (is (= (get-in (#'server/ipp-handler config req) [:headers "content-type"]) "application/ipp"))))
