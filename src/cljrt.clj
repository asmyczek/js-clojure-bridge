;; Copyright (c) 2010, Mu Dynamics.
;; by Adam Smyczek
;; All rights reserved.
;;  
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;     * Redistributions of source code must retain the above copyright
;;       notice, this list of conditions and the following disclaimer.
;;     * Redistributions in binary form must reproduce the above copyright
;;       notice, this list of conditions and the following disclaimer in the
;;       documentation and/or other materials provided with the distribution.
;;     * Neither the name of the "Mu Dynamics" nor the
;;       names of its contributors may be used to endorse or promote products
;;       derived from this software without specific prior written permission.
;;  
;; THIS SOFTWARE IS PROVIDED BY COPYRIGHT HOLDERS AND CONTRIBUTORS ''AS IS'' AND
;; ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
;; WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
;; ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
;; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
;; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
;; ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
;; SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

(ns 
  #^{:doc 
     "JS-Clojure-Bridge
     CljRT provides access to Clojure namespaces
     from JavaScript running on Rhino."
     :author "Adam Smyczek"} 
  cljrt
  (:import (org.mozilla.javascript 
             Context 
             NativeObject 
             NativeArray)))

(defn- render [v]
  "A StringBuffer wrapper,
  renders cascading vectors and sequences." 
  (let [sb (StringBuffer.)
        rn (fn rn [v]
            (if (or (vector? v) (seq? v))
            (doseq [e v] (rn e))
              (.append sb v)))]
  (rn v)
  (str sb)))

(defn- sanitize
  "Sanitizes Clojure names."
  [s]
  (.replaceAll (str s) "[\\W_]+" "_"))

(defn- render-packages
  "Render JavaScript packages in the form:
   var clj = clj || {};
   clj.<name space> = ..."
  [nsname]
  (let [names (vec (map sanitize (.split nsname "\\.")))
        part  (fn [i] (interpose "." (take i names)))]
    ["var clj = clj || {};\n"
     (for [i (range 1 (count names))]
       ["clj." (part i) " = clj." (part i) " || {}; \n"])]))

(defn- render-args
  "Render JavaScript argument list."
  [args f]
    (letfn [(is-arg [v] (not (= "&" (str v))))
            (f-san  [v] (f (sanitize v)))]
    (interpose ", " (map f-san (filter is-arg (first args))))))

(defn- render-function
  "Render a function call."
  [v]
  (let [m        (meta v)
        args     (:arglists m)
        arg-conv (fn [s] (str "rt.toClj(" s ")"))]
    (if (> (count args) 1) 
      (throw (Exception. "Only one method per function supported!"))
        ["		" (sanitize (:name m)) " : "
         "function(" (render-args args identity) ") "
         "{ return rt.toJs(rt.func('" (:name m) "').invoke(" 
           (render-args args arg-conv) "), this); }"]
      )))

(defn- filter-public-fn 
  "Filter public functions."
  [ns]
  (let [vs (map second (ns-publics ns))]
    (filter #(-> % meta :arglists nil? not) vs)))

(defn- render-ns 
  "Render namespace."
  [ns]
  (let [vs (filter-public-fn ns)]
    (render [
      (render-packages (name ns))
      "clj." (sanitize (name ns)) " = function() {\n"
      "	var rt = new Packages.CljRT('" (name ns) "');\n"
      "	return {\n"
      (interpose ",\n" (map render-function vs))
      "\n	};\n"
      "}();"
      ])))

;; ----------------------------------
;; Serialization Rhino JS <-> Clojure

(defn- lazy-arr 
  "Convers a NativeArray to a lazy Clojure sequence."
  [arr ids]
  (if (empty? ids)
    nil 
    (let [nx (.get arr (first ids) nil)]
      (lazy-seq (cons nx (lazy-arr arr (rest ids)))))))

(defn to-clj 
  "Convert Rhino JavaScript object to Clojure data."
  [obj]
  (letfn [(to-key     [s]   (keyword (sanitize s)))
          (value-for  [id]  (.get obj id nil))
          (keys-for   [ids] (map to-key ids))
          (values-for [ids] (map value-for ids))]
    (cond 
      (instance? NativeObject obj) 
        (let [ids (.getIds obj)] 
          (zipmap (keys-for ids) (values-for ids)))
      (instance? NativeArray obj)
        (let [ids (.getIds obj)]
          (lazy-arr obj ids))
      :else obj)))

(defn to-js 
  "Convert Clojure data to Rhino JavaScript object."
  [data scope]
  (let [ctx (Context/getCurrentContext)]
    (.setJavaPrimitiveWrap (.getWrapFactory ctx) false)
    (letfn [(to-name [s]       (if (instance? clojure.lang.Named s) 
                                 (name s)
                                 (str s)))
            (new-obj []        (.newObject ctx scope))
            (put     [k v obj] (.put obj (to-name k) obj v))
            (to-arr  [arr]     (to-array (map #(to-js % scope) arr)))]
      (cond 
        (map? data) 
          (let [obj (new-obj)] 
            (doseq [k (keys data)] (put k (to-js (data k) obj) obj)) 
            obj)
        (or (seq? data) (vector? data)) 
            (.newArray ctx scope (to-arr data))
        :else (Context/javaToJS data scope)))))

;; ----------------------
;; Clj-RT implementations

(gen-class
  :name         "CljRT"
  :state        state
  :init         init
  :constructors {[String] []}
  :methods
    [#^{:static true} 
     [importNs  [String org.mozilla.javascript.Scriptable] Object] 
     #^{:static true}
     [renderNs  [String]                                   Object] 
     #^{:static true}
     [reloadNs  [String]                                   Object]
     [reloadAll []                                         Object]
     [func      [String]                                   clojure.lang.Var] 
     [toClj     [Object]                                   Object] 
     [toJs      [Object org.mozilla.javascript.Scriptable] Object]])

(defn -init
  [nsname]
  [[] (ref (symbol nsname))])

(defn -importNs 
  "Generates JavaScript packages for a namespace in current scope."
  [nsname scope]
   (let [ns (symbol nsname)]
     (require ns)
     (let [js  (render-ns ns)
           ctx (Context/getCurrentContext)
           s   (.compileString ctx js nsname 1 nil)]
       (.exec s ctx scope)
       nil)))

(defn -renderNs 
  "Render function for debugging."
  [nsname]
   (let [ns (symbol nsname)]
     (require ns)
     (render-ns ns)))

(defn -reloadNs
  "Reload namespace."
  [nsname]
   (let [ns (symbol nsname)]
     (require :reload ns)))

(defn -reloadAll
  "Reload all namespaces."
  []
  (require :reload-all))

(defn -func 
  "Resolves function var for function name in current namespace."
  [this fnname]
  (let [ns (find-ns @(.state this))] 
    (ns-resolve ns (symbol fnname))))

(defn -toClj
  "to-clj accessor."
  [this obj]
  (to-clj obj))

(defn -toJs
  "to-js accessor."
  [this obj scope]
  (to-js obj scope))

