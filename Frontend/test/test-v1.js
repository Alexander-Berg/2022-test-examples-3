var test = require('tape');
var v1 = require('../v1');

test('v1 function', function(t) {
    t.equal(typeof v1, 'function', 'should be a function');
    t.equal(typeof v1.isValid, 'function', 'should have isValid method');

    test('v1 uid version (FULL_TOKEN_VERSION)', function(t) {
        var token = v1({ uid: '123142323' });
        t.equal(typeof token, 'string', 'returns string');
        t.equal(token[0], 'u', 'starts with `u`');
        t.equal(token.length, 33, 'contains 33 characters (`u` + md5 hash)');
        t.end();
    });

    test('v1 yandexuid version (YUID_TOKEN_VERSION)', function(t) {
        var token = v1({ yandexuid: '6447714881391768016' });
        t.equal(typeof token, 'string', 'returns string');
        t.equal(token[0], 'y', 'starts with `y`');
        t.equal(token.length, 33, 'contains 33 characters (`y` + md5 hash)');
        t.end();
    });

    test('v1.isValid', function(t) {
        var opts = { yandexuid: '6447714881391768016', days: 100 };
        var foo = v1(opts);
        t.ok(v1.isValid(foo, opts), 'should return true on same token');

        var bar = v1(opts);
        t.ok(v1.isValid(bar, { yandexuid: '6447714881391768016', days: 101 }), 'should accept tokens from yesterday');
        t.end();
    });

    test('v1.salt', function(t) {
        var opts = { yandexuid: '6447714881391768016', salt: 'mysalt' };
        var foo = v1(opts);
        t.ok(v1.isValid(foo, opts), 'should return true on same token with salt');

        var bar = v1(opts);
        t.notOk(v1.isValid(bar, { yandexuid: '6447714881391768016', salt: 'wrong' }), 'should deny tokens with wrong salt');
        t.end();
    });

    t.end();
});
