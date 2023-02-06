const Url = require('url');
const { CGI: CGIRef } = require('@yandex-int/si.utils/CGI/CGI');
const { createSuite } = require('./benchmark-utils');
const { CGI } = require('../CGI/CGI');

const url = 'https://yandex.ru:1111/search/?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2&lr=213&win=489&redircnt=1627565265.1&src=suggest_B&rnd=1';
const urlShort = 'https://yandex.ru:1111/search/?text=test';

let uid = 1;

createSuite('CGI constructor - long url')
    .add('ctor reference (warm-up)', function() {
        CGIRef(url + uid++);
    })
    .add('ctor current (warm-up)', function() {
        CGI(url + uid++);
    })
    .add('ctor reference', function() {
        CGIRef(url + uid++);
    })
    .add('ctor current', function() {
        CGI(url + uid++);
    })
    .run({ async: false });

createSuite('CGI constructor - short url')
    .add('ctor reference (warm-up)', function() {
        CGIRef(urlShort + uid++);
    })
    .add('ctor current (warm-up)', function() {
        CGI(urlShort + uid++);
    })
    .add('ctor reference', function() {
        CGIRef(urlShort + uid++);
    })
    .add('ctor current', function() {
        CGI(urlShort + uid++);
    })
    .run({ async: false });

createSuite('Url.parse()')
    .add('long url', function() {
        Url.parse(url + uid++, false, true);
    })
    .add('short url', function() {
        Url.parse(urlShort + uid++, false, true);
    })
    .run({ async: false });
