let test = require('tape');
let got = require('got');
let Clck = require('..');

test('encrypted', function(t) {
    t.plan(8);

    let defaultOpts = {
        crypt: 2,
        params: 'dtype=stred/ab=true/*',
        yandexuid: 111,
        key: 'W50/PxLsdRldOKXKklW4Ng==',
        keyNo: 5,
    };

    let clck = new Clck();

    let opts = Object.assign({}, defaultOpts, { key: Buffer.from(defaultOpts.key, 'base64') });
    let clckWithOpts = new Clck(opts);

    let optsWithParams = Object.assign({}, defaultOpts, { params: {
        dtype: 'stred',
        ab: true,
    } });
    let clckWithParams = new Clck(optsWithParams);

    let optsWithJsRedir = Object.assign({}, defaultOpts, { jsredir: true });
    let clckWithJsRedir = new Clck(optsWithJsRedir);

    got('https:' + clck.getClckUrl('http://www.kinopoisk.ru/docs/usage/'), function(err, body) {
        t.error(err, 'should not get error');
        t.ok(/www.kinopoisk.ru/.test(body), 'should get Kinopoisk');
    });

    got('https:' + clckWithOpts.getClckUrl('http://www.kinopoisk.ru/docs/usage/'), function(err, body) {
        t.error(err, 'should not get error with Buffer as key');
        t.ok(/www.kinopoisk.ru/.test(body), 'should get Kinopoisk with Buffer as key');
    });

    got('https:' + clckWithParams.getClckUrl('http://www.kinopoisk.ru/docs/usage/'), function(err, body) {
        t.error(err, 'should not get error with Object as params');
        t.ok(/www.kinopoisk.ru/.test(body), 'should get Kinopoisk with Object as params');
    });

    got('https:' + clckWithJsRedir.getClckUrl('http://www.kinopoisk.ru/docs/usage/'), function(err, body) {
        t.error(err, 'should not get error with jsredir');
        t.ok(/www.kinopoisk.ru/.test(body), 'should get Kinopoisk with jsredir');
    });
});

test('query params in url', function(t) {
    let opts = {
        crypt: 2,
        params: 'dtype=stred/ab=true/*',
        yandexuid: 111,
        key: 'W50/PxLsdRldOKXKklW4Ng==',
        keyNo: 5,
    };
    let clck = new Clck(opts);

    got('https:' + clck.getClckUrl('https://passport.yandex.ru?testarg=1&retpath=' + encodeURIComponent('https://ya.ru/teststring')), function(err, body) {
        t.error(err, 'should not get error');
        t.ok(/teststring/.test(body), 'should keep query params');
        t.end();
    });
});
