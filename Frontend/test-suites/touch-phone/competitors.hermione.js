'use strict';

const PO = require('../../page-objects/common/index').PO;
const competitors = require('../common/competitors');
const block = PO.searchengines;
const query = 'test';

[query, ''].forEach(function(q) {
    hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
    hermione.only.notIn('winphone', 'тест не работает в winphone');
    competitors(q, block, [
        {
            name: 'Bing',
            selector: PO.searchengines.bing(),
            url: 'https://m.bing.com/search?q=' + q,
            family: '&adlt=strict',
            counter_suffix: 'bing'
        },
        {
            name: 'Google',
            selector: PO.searchengines.google(),
            url: 'https://www.google.com/m/search?q=' + q,
            family: '&safe=strict',
            counter_suffix: 'google'
        },
        {
            name: 'Mail.ru',
            selector: PO.searchengines.mail(),
            url: 'https://go.mail.ru/msearch?q=' + q,
            counter_suffix: 'mail'
        }
    ]);
});
