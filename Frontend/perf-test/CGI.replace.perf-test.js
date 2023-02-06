const { CGI: CGIRef } = require('@yandex-int/si.utils/CGI/CGI');
const { createSuite } = require('./benchmark-utils');
const { CGI } = require('../CGI/CGI');

const url = 'https://yandex.ru:1111/search/?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2';
const cgi = CGI(url);
const cgiRef = CGIRef(url);

createSuite()
    .add('replace reference (warm-up)', function() {
        cgiRef
            .replace('text', 'textValue')
            .replace('exp_flags', 'flagValue')
            .replace('unknown', 'unknownValue');
    })
    .add('replace current (warm-up)', function() {
        cgi
            .replace('text', 'textValue')
            .replace('exp_flags', 'flagValue')
            .replace('unknown', 'unknownValue');
    })
    .add('replace reference', function() {
        cgiRef
            .replace('text', 'textValue')
            .replace('exp_flags', 'flagValue')
            .replace('unknown', 'unknownValue');
    })
    .add('replace current', function() {
        cgi
            .replace('text', 'textValue')
            .replace('exp_flags', 'flagValue')
            .replace('unknown', 'unknownValue');
    })
    .run({ async: false });

createSuite()
    .add('replace reference batch (warm-up)', function() {
        cgiRef
            .replace('text', 'textValue', 'exp_flags', 'flagValue', 'unknown', 'unknownValue');
    })
    .add('replace current batch (warm-up)', function() {
        cgi
            .replace('text', 'textValue', 'exp_flags', 'flagValue', 'unknown', 'unknownValue');
    })
    .add('replace reference batch', function() {
        cgiRef
            .replace('text', 'textValue', 'exp_flags', 'flagValue', 'unknown', 'unknownValue');
    })
    .add('replace current batch', function() {
        cgi
            .replace('text', 'textValue', 'exp_flags', 'flagValue', 'unknown', 'unknownValue');
    })
    .run({ async: false });
