// A basic assert function
var assert = function(is, expected, msg) {
  var passed = is === expected;
  if (passed) {
    print('PASSED: ' + msg);
  } else {
    print('FAILED: ' + msg + ', expected ' + expected + ' but is ' + is + '.');
  }
}

// Import test package
Packages.CljRT.importNs('test-ns', this);
var t = clj.test_ns;

// Run tests
assert(t.testCall('from'), 'from-to', 'Simple call.');
assert(t.noArgs(), 'no args', 'No args.');
assert(t.sanitize_names(), 'sanitized', 'Sanitize names.');
assert(t.and_args(['a', 'b', 'c']), 3, 'Clojure & args.');
assert(t.array_arg(['a', 'b', 'c']), 3, 'Array arg.');
assert(t.json_arg({ name : 'Bob' }), 'name: Bob', 'JSON arg.');
assert(t.number_result(10) + 5, 20, 'Number result.');
assert(t.array_result()[1] , 'b', 'Array result.');
assert(t.object_result().name , 'Bob', 'Object result.');

