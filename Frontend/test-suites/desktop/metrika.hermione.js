'use strict';

const PO = require('../../page-objects/desktop').PO,
    METRIKA_ID = 33250276;

specs('Счётчик Метрики', function() {
    it('Проверка инициализации счётчика для домена', function() {
        return this.browser
            .yaOpenSerp({ text: 'test' })
            .yaExecute(function(counter) {
                var metrikaInstance = window['yaCounter' + counter];

                return metrikaInstance instanceof Ya.Metrika;
            }, METRIKA_ID).then(result => assert.isTrue(result.value, 'счётчик не был проинициализирован'));
    });

    hermione.only.notIn(/ie\d+/, 'В IE нельзя сматчится на noscript');
    it('Проверка пиксельного счётчика с синхронизацией куки', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', tld: 'com.tr' })
            .yaExecute(function(counter) {
                var metrikaInstance = window['yaCounter' + counter];

                return metrikaInstance instanceof Ya.Metrika;
            }, METRIKA_ID).then(result => assert.isTrue(result.value, 'счётчик не был проинициализирован'))
            .yaGetHTML(PO.metrikaNoScript())
            .then(counter =>
                assert.equal(counter, '<noscript><div style="background:url(//mc.yandex.ru/watch/' + METRIKA_ID +
                    ')"></div><div style="background:url(//mc.yandex.com.tr/sync_cookie_image_check)"></div>' +
                    '</noscript>')
            );
    });
});
