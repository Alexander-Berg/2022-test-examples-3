'use strict';

const PO = require('../../../page-objects/touch-phone/index').PO;

specs('Колдунщик такси', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=заказать такси в москве')
            .yaWaitForVisible(PO.taxi(), 'Не появился колдунщик такси');
    });

    it('Проверка ссылок и счётчиков', function() {
        const TAXI_URL = 'https://m.taxi.yandex.ru/';

        return this.browser
            .yaCheckSnippet(PO.taxi, {
                title: {
                    url: TAXI_URL,
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="taxi"]/title'
                    }
                },
                greenurl: [{
                    url: TAXI_URL,
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="taxi"]/path/urlnav'
                    }
                }]
            })
            .yaCheckLink(PO.taxi.tariffsButton()).then(url =>
                this.browser.yaCheckURL(url, TAXI_URL, 'Сломана ссылка в кнопке "Тарифы"')
            )
            .yaCheckBaobabCounter(PO.taxi.tariffsButton(), {
                path: '/$page/$main/$result[@wizard_name="taxi"]/tariff'
            })
            .yaCheckLink(PO.taxi.orderButton()).then(url =>
                this.browser.yaCheckURL(url, TAXI_URL, 'Сломана ссылка в кнопке "Вызвать такси"')
            )
            .yaCheckBaobabCounter(PO.taxi.orderButton(), {
                path: '/$page/$main/$result[@wizard_name="taxi"]/order'
            });
    });
});
