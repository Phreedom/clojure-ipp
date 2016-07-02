(ns ipp.enums
  (:require
    [clojure.set :refer [map-invert]]))

(defn ^:private reserved-range-map [first last]
  (into {} (map (fn [v] [v (keyword (str "reserved-" v))]) (range first last))))

;;; attribute value types

(def tag-to-character-string-type {
  0x41 :text-without-language
  0x42 :name-without-language
  0x44 :keyword
  0x45 :uri
  0x46 :uri-scheme
  0x47 :charset
  0x48 :natural-language
  0x49 :mime-media-type})

(def character-string-type-to-tag (map-invert tag-to-character-string-type))

(def tag-to-integer-type {
  0x21 :integer
  0x22 :boolean
  0x23 :enum})

(def integer-type-to-tag (map-invert tag-to-integer-type))

; we must correctly recognize and skip the types we don't know
(def tag-to-reserved-type
  (merge (reserved-range-map 0x10 0x20)
         (reserved-range-map 0x24 0x40)
         (reserved-range-map 0x43 0x43)
         (reserved-range-map 0x4a 0xff)))

(def reserved-type-to-tag (map-invert tag-to-reserved-type))

(def tag-to-attribute-value-type (merge tag-to-integer-type
                                        tag-to-character-string-type
                                        tag-to-reserved-type))

(def attribute-value-type-to-tag (map-invert tag-to-attribute-value-type))

;;; attribute groups

(def supported-attribute-groups
  (merge {
      0x01 :operation-attributes
      0x02 :job-attributes
      0x04 :printer-attributes
      0x05 :unsupported-attributes}
    ; these values are defined in the spec as attribute groups, and we must skip them correctly
    (reserved-range-map 0x06 0x0f)))

(def attribute-group-type-to-code (map-invert supported-attribute-groups))

(def end-of-attributes-tag 3)

;;; operations

(def tag-to-operation
  { 0x02 :print-job
    0x04 :validate-job
    0x08 :cancel-job
    0x09 :get-job-attributes
    0x0A :get-jobs
    0x0B :get-printer-attributes})

(def operation-to-tag (map-invert tag-to-operation))

;;; states

(def  printer-state
  {:idle 3
   :processing 4
   :stopped 5})

(def job-state
  {:pending 3
   :pending-held 4
   :processing 5
   :processing-stopped 6
   :canceled 7
   :aborted 8
   :completed 9})

(def status-to-code
  {:successful 0x00
   :server-error-operation-not-supported 0x0501
   :client-error-bad-request 0x0400})


