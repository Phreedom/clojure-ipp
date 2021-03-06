(ns ipp.serializer
  (:refer-clojure :exclude [bytes])
  (:require [ipp.enums :refer :all]))

(def compose concat)
(def mapcompose mapcat)

(defn int8 [v]
  {:pre [(integer? v)]}
  [(unchecked-byte v)])

(defn bytes [v]
;  {:pre [(sequential? v)]} ;we also want to write byte-arrays and they aren't sequential
  (mapcompose int8 v))

(defn int16 [v]
  {:pre [(integer? v)]}
  (compose (int8 (clojure.lang.Numbers/unsignedShiftRightInt v 8))
           (int8 v)))

(defn int32 [v]
  {:pre [(integer? v)]}
  (compose (int8 (clojure.lang.Numbers/unsignedShiftRightInt v 24))
           (int8 (clojure.lang.Numbers/unsignedShiftRightInt v 16))
           (int8 (clojure.lang.Numbers/unsignedShiftRightInt v 8))
           (int8 v)))

(defn ipp-string [str]
  {:pre [(string? str)]}
  (let [arr (.getBytes str "UTF-8")]
    (compose
      (int16 (count arr))
      (bytes arr))))

(defn attribute-value [{:keys [type value]}]
  (condp get type
    character-string-type-to-tag
      (ipp-string value)
    boolean-type-to-tag
      (compose (int16 1)
               (int8 (if value 1 0)))
    integer-type-to-tag
      (compose (int16 4)
               (int32 value))
    reserved-type-to-tag
      (ipp-string value)
    :else
      (throw (IllegalArgumentException.
                "unsupported attribute value type"))))

(defn extra-value [{:keys [type value] :as orig}]
  (compose (int8 (attribute-value-type-to-tag type))
           (int16 0)
           (attribute-value orig)))

(defn attribute [[attr [value & extra]]]
  {:pre [(map? value)]}
  (compose
    (int8 (attribute-value-type-to-tag (:type value)))
    (ipp-string attr)
    (attribute-value value)
    (mapcompose extra-value extra)))

(defn attribute-group [{:keys [group attrs]}]
  {:pre [(map? attrs)]}
  (compose
    (int8 (attribute-group-type-to-code group))
    (mapcompose attribute attrs)))

(defn ipp-response [{:keys [version-major version-minor op request-id groups body]}]
  {:pre [(sequential? groups)]}
  (compose
    (int8 version-major)
    (int8 version-minor)
    (int16 op)
    (int32 request-id)
    (mapcompose attribute-group groups)
    (int8 end-of-attributes-tag)
    (bytes body)))

(def serialize-ipp-response ipp-response)
