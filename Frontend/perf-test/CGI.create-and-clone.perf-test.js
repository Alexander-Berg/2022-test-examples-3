const { CGI: CGIRef } = require('@yandex-int/si.utils/CGI/CGI');
const { createSuite } = require('./benchmark-utils');
const { CGI } = require('../CGI/CGI');

const url = 'https://yandex.ru:1111/search/?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2';

createSuite()
    .add('create and clone reference (warm-up)', function() {
        CGIRef(url).clone();
    })
    .add('create and clone current (warm-up)', function() {
        CGI(url).clone();
    })
    .add('create and clone reference', function() {
        CGIRef(url).clone();
    })
    .add('create and clone current', function() {
        CGI(url).clone();
    })
    .run({ async: true });
