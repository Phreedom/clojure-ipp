(ns ipp.parser
  (:require
    [ipp.enums :refer :all]
    [zetta.core :as zetta :refer [<$>]]
    [zetta.parser.seq :as zseq]
    [zetta.parser.string :as zstr]
    [zetta.combinators :as zc]))

(def int16
  (<$> #(bit-or (bit-shift-left (bit-and 0xff (int %1)) 8) (bit-and 0xff (int %2)))
       zseq/any-token
       zseq/any-token))

(def int32
  (<$> #(bit-or (bit-shift-left %1 16) %2)
       int16
       int16))

(defn to-string
  "Convert byte sequence to string"
  [byte-array]
  (apply str (map #(char (bit-and % 0xff)) byte-array)))

(def fixed-string
  (zetta/do-parser
    len <- int16
    str <- (zseq/take len)
    (zetta/always (to-string str))))

(defn attribute-value [type]
  (cond
    (get tag-to-character-string-type type)
      (zetta/do-parser
        value <- fixed-string
        (zetta/always {:type (get tag-to-character-string-type type) :value value}))
    (= type (:boolean integer-type-to-tag))
      (zetta/do-parser
        (zseq/take-with 2 #(= % [0 1])) ;size = 1
        value <- (zseq/satisfy? #(or (= % 0) (= % 1))) ;value = 1|0
        (zetta/always {:type :boolean :value (if (= 1 value) true false)}))
    (get tag-to-integer-type type)
      (zetta/do-parser
        (zseq/take-with 2 #(= % [0 4])) ;size = 4
        value <- int32
        (zetta/always {:type (get tag-to-integer-type type) :value value}))
    (get tag-to-reserved-type type)
      (zetta/do-parser
        value <- fixed-string
        (zetta/always {:type (get tag-to-reserved-type type) :value value}))
    :else
      (zetta/fail-parser (str "Invalid attribute type: " type))))

(def additional-value
  (zetta/do-parser
    ;FIXME: what an ugly-looking look-ahead
    prefix <- (zseq/take-with 3 #(and (get tag-to-attribute-value-type (first %))
                                      (= [0 0] (rest %))))
    (attribute-value (first prefix))))

(def attribute
  (zetta/do-parser
    type <- (zseq/satisfy? #(get tag-to-attribute-value-type %))
    name <- fixed-string
    value <- (attribute-value type)
    extra-values <-(zc/many additional-value)
    (zetta/always {name (concat [value] extra-values)})))

(def attribute-group
  (<$> (fn [type attrs] {:group (get supported-attribute-groups type) :attrs (into {} attrs)})
       (zseq/satisfy? #(get supported-attribute-groups %))
       (zc/many attribute)))

(def ipp-request
  (<$> (fn [v-maj v-min op rid groups _] {
         :version_major v-maj
         :version_minor v-min
         :op op
         :groups groups
         :request_id rid})
       zseq/any-token
       zseq/any-token
       int16
       int32
       (zc/many attribute-group)
       (zseq/satisfy? #(= % end-of-attributes-tag))))

(defn parse-ipp-request [body]
  (let [req (zetta/parse-once ipp-request body)]
    (when (zetta/done? req)
      (assoc (:result req)
             :body (byte-array (:remainder req))))))
