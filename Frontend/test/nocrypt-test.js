let test = require('tape');
let got = require('got');
let Clck = require('..');

test('plain', function(t) {
    let clck = new Clck();

    got('https:' + clck.getClckUrl('https://beta.maps.yandex.ru'), function(err, body) {
        t.error(err, 'should not get error');
        t.ok(/Яндекс.Карты/.test(body), 'should get Yandex.maps');
        t.end();
    });
});

test('query params in url', function(t) {
    let clck = new Clck();

    got('https:' + clck.getClckUrl('https://passport.yandex.ru?testarg=1&retpath=' + encodeURIComponent('https://ya.ru/teststring')), function(err, body) {
        t.error(err, 'should not get error');
        t.ok(/teststring/.test(body), 'should keep query params');
        t.end();
    });
});
