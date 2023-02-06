const { CGI: CGIRef } = require('@yandex-int/si.utils/CGI/CGI');
const { createSuite } = require('./benchmark-utils');
const { CGI } = require('../CGI/CGI');

const url = 'https://yandex.ru:1111/search/?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2';
const cgi = CGI(url);
const cgiRef = CGIRef(url);

createSuite()
    .add('clone reference (warm-up)', function() {
        cgiRef.clone();
    })
    .add('clone current (warm-up)', function() {
        cgi.clone();
    })
    .add('clone reference', function() {
        cgiRef.clone();
    })
    .add('clone current', function() {
        cgi.clone();
    })
    .run({ async: true });
