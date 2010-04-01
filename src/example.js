// Import any-ns namespace
Packages.CljRT.importNs("any-ns", this);

// Call Clojure functions in clj.any_ns
var r = clj.any_ns.callClojure("Calling Clojure");
print(r);

