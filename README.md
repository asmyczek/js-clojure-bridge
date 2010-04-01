JavaScript-Clojure-Bridge
=========================

JS-Clojure-Bridge provides access to Clojure from JavaScript
running on Rhino. Calling Clojure functions is as simple as:

  1. importing a namespace into JavaScript
     `Packages.CljRT.importNs("my.namespace", this);`
  2. and calling its public functions:
     `var result = clj.my.namespace.my_function('Calling Clojure');`.

`importNs` generates JavaScript modules and function calls to Clojure RT that match
to names of the imported namespace. All function arguments are converted to appropriate
Clojure data objects and the results back to JavaScript types. To reload a changed
namespace into a running VM use `CljRT.reloadNs("my.namespace")` or 
`CljRT.reloadAll()`. Currently only one method per function is supported.

Build with ant.

For details see example and unit tests.

