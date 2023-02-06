'use strict';

const PO = require('../../page-objects/common/index').PO;
const competitors = require('../common/competitors');
const block = PO.searchengines;
const query = 'test';

competitors(query, block, [
    {
        name: 'Bing',
        selector: PO.searchengines.bing(),
        url: 'https://www.bing.com/search?q=' + query,
        family: '&adlt=strict'
    },
    {
        name: 'Google',
        selector: PO.searchengines.google(),
        url: 'https://www.google.ru/search?ie=UTF-8&hl=ru&q=' + query,
        family: '&safe=strict'
    },
    {
        name: 'Mail.ru',
        selector: PO.searchengines.mail(),
        url: 'https://go.mail.ru/search?mailru=1&q=' + query
    }
]);

specs('Поиск в других системах на пустом запросе', function() {
    it('отсутствует', function() {
        return this.browser
            .yaOpenSerp({ text: '', exp_flags: 'hide-popups=1' })
            .yaShouldNotExist(block());
    });
});
