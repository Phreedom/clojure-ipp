(ns ipp.serializer-test
  (:require [clojure.test :refer :all]
            [ipp.serializer :as serializer]))

(deftest serialize-int16
  (is (= (serializer/int16 0xabc) (map unchecked-byte [0x0a 0xbc]))))

(deftest serialize-int32
  (is (= (serializer/int32 0xabcdef0) (map unchecked-byte [0x0a 0xbc 0xde 0xf0]))))