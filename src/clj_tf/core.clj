(ns clj-tf.core
  (:require [clojure.java.shell :refer [sh]]
            [jsonista.core :as json]
            [clojure.string :as str]))

(defn sh! [& args]
  (let [result (apply sh args)]
    (when-not (zero? (:exit result))
      (throw (Exception. (str (:out result) \newline (str (:err result))))))
    result))

(defonce schema-atom (atom nil))

(defn load-schema []
  (let [value @schema-atom]
    (if-not value
      (reset! schema-atom (json/read-value (:out (sh! "terraform" "providers" "schema" "-json"))
                                           (json/object-mapper {:decode-key-fn true})))
      value)))

(def default-skip-node
  #{;; Intermediate nodes
    :block :block_types :attributes
    ;; Leaf nodes
    :type :description_kind :nesting_mode :max_items :optional :computed})

(defn dive [acc-atom data path skip-node]
  (if (map? data)
    (doseq [[k v] data]
      (dive acc-atom
            (get data k)
            (if (not (skip-node k))
              (conj path k)
              path)
            skip-node))
    (swap! acc-atom conj path)))

(defn collect-paths
  ([data]
   (collect-paths data default-skip-node))
  ([data skip-node]
   (let [acc (atom #{})]
     (dive acc data [] skip-node)
     @acc)))

(defn make-keywords [paths]
  (doseq [path paths]
    (let [path-name (last path)
          path-namespace (butlast path)]
      (keyword (str/join "." (map name path-namespace)) (name path-name)))))

(defn init-namespaced-keywords []
  (let [;; Reading via encoding keys to keywords gives us keywords without namespace, which might be useful too
        schema (load-schema)]
    (doseq [[provider-schema-key provider-schema-value] (:provider_schemas schema)]
      (make-keywords (collect-paths (:resource_schemas provider-schema-value)))
      (make-keywords (collect-paths (:data_source_schemas provider-schema-value)))
      (let [k provider-schema-key
            k-namespace (namespace k)
            k-name (name k)
            provider-key (keyword (str (.replace k-namespace "/" ".")
                                       "."
                                       (.replace k-name "/" ".")))]
        (make-keywords (collect-paths {provider-key (:provider provider-schema-value)} (conj default-skip-node :description)))))))

(defn write-json
  "Writes the infrastructure data as JSON, with namespaces removed from keyword keys"
  [iac filename]
  (spit filename (json/write-value-as-string iac (json/object-mapper {:encode-key-fn (fn [k]
                                                                                       (name k))}))))
