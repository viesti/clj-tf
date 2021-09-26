(ns clj-tf.core
  (:require [clojure.java.shell :refer [sh]]
            [jsonista.core :as json]))

(defn sh! [& args]
  (let [result (apply sh args)]
    (when-not (zero? (:exit result))
      (throw (Exception. (str (:out result) \newline (str (:err result))))))
    result))

(def schema-atom (atom nil))

(defn load-schema []
  (let [value @schema-atom]
    (if-not value
      (reset! schema-atom (json/read-value (:out (sh! "terraform" "providers" "schema" "-json"))
                                           (json/object-mapper {:decode-key-fn true})))
      value)))

(defn init-namespaced-keywords []
  (let [;; Reading via encoding keys to keywords gives us keywords without namespace, which might be useful too
        schema (load-schema)]
    (doseq [[_provider-schema-key provider-schema-value] (:provider_schemas schema)]
      (doseq [[resource-type resource-schema] (:resource_schemas provider-schema-value)]
        (let [block (:block resource-schema)
              ;; TODO: Recursively walk blocks, perhaps make keywords like :aws_cloudfront_distribution-origin-custom_header/name, so split nested blocks with "-"
              block_types (:block_types block)
              attributes (:attributes block)]
          ;; Make keywords just to internalize resource/<block or attribute> keywords
          (doseq [[k _v]  block_types]
            (keyword (name resource-type) (name k)))
          (doseq [[k _v] attributes]
            (keyword (name resource-type) (name k))))))))

(defn write-json
  "Writes the infrastructure data as JSON, with namespaces removed from keyword keys"
  [iac filename]
  (spit filename (json/write-value-as-string iac (json/object-mapper {:encode-key-fn (fn [k]
                                                                                       (name k))}))))
