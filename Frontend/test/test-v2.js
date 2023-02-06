var test = require('tape');
var v2 = require('../v2');

test('v2 function', function(t) {
    t.equal(typeof v2, 'function', 'should be a function');
    t.equal(typeof v2.isValid, 'function', 'should have isValid method');

    test('v2 options.salt is required', function(t) {
        t.throws(function() { v2() });
        t.doesNotThrow(function() { v2({ salt: '1' }) });
        t.end();
    });

    test('v2 should return valid key', function(t) {
        var key = v2({ salt: '1', uid: '1', yandexuid: '1' });

        var ts = parseInt(key.split(':').pop());
        var sha = key.split(':').shift();

        t.ok(ts, 'should ends with timestamp');
        t.equal(sha.length, 40, 'should contain sha1');

        t.end();
    });

    test('v2.isValid', function(t) {
        var opts, foo;

        opts = { salt: '1', yandexuid: '6447714881391768016' };
        foo = v2(opts);
        t.ok(v2.isValid(foo, opts), 'should validate same token');

        opts = { lifetime: 10, salt: '1', yandexuid: '6447714881391768016' };
        foo = v2(opts);

        setTimeout(function() {
            t.ok(v2.isValid(foo, opts), 'should validate same token after a while');
        }, 1);

        setTimeout(function() {
            t.notOk(v2.isValid(foo, opts), 'should invalidate token by lifetime');
            t.end();
        }, 50);
    });

    t.end();
});
