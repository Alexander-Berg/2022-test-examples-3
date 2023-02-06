let test = require('tape');
let Clck = require('..');

test('options', function(t) {
    let clck = new Clck({ crypt: 1 });
    let clckWithKeyNo = new Clck({ crypt: 2, keyNo: 5 });
    let clckWithKey = new Clck({ crypt: 2, key: 'key' });

    t.throws(function() {
        clck.getClckUrl('https://beta.maps.yandex.ru');
    }, /Invalid crypt version/, 'should throw with invalid crypt version');

    t.throws(function() {
        clckWithKeyNo.getClckUrl('https://beta.maps.yandex.ru');
    }, /key is not defined, but crypt is used/, 'should throw with encryption and without key');

    t.throws(function() {
        clckWithKey.getClckUrl('https://beta.maps.yandex.ru');
    }, /keyNo is not defined, but crypt is used/, 'should throw with encryption and without keyNo');

    let clckWithParams = new Clck({ params: { dtype: 'stred', ab: true } });
    let clckWithJsRedir = new Clck({ jsredir: true });
    let clckWithExtraParams = new Clck({ crypt: 2, keyNo: 5, key: 'key', extParams: { from: 'fromtest', text: 'test' } });
    let clckWithReferrer = new Clck({ crypt: 2, keyNo: 5, key: 'key', referrer: 'http://yandex.ru' });

    t.ok(clckWithParams.getClckUrl('https://beta.maps.yandex.ru').match(/\/dtype=stred\/ab=true\/\*/), 'should build redir with params object');
    t.ok(clckWithJsRedir.getClckUrl('https://beta.maps.yandex.ru').match(/\/jsredir\?state=\*/), 'should build jsredir');
    t.ok(clckWithExtraParams.getClckUrl('https://beta.maps.yandex.ru').match(/from=fromtest&text=test/), 'should build redir with extra params');
    t.ok(clckWithReferrer.getClckUrl('https://beta.maps.yandex.ru').match(/ref=t1g8aF6NqoTlivXxS3WOnw/), 'should build redir with referrer');

    t.end();
});
