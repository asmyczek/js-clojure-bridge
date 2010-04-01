(ns test-ns)

(defn testCall [from]
  (str from "-" "to"))

(defn noArgs []
  "no args")

(defn sanitize-names []
  "sanitized")

(defn and-args [ & arr ]
  (count (first arr)))

(defn array-arg [arr]
  (count arr))

(defn json-arg [json]
  (str "name: " (:name json)))

(defn number-result [i]
  (+ i 5))

(defn array-result []
  [\a \b \c])

(defn object-result []
  { :name "Bob" })

