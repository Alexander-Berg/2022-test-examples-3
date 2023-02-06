'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Колдунщик погоды / на выходные', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=погода на выходные&lr=213')
            .yaWaitForVisible(PO.weather(), 'Не появился колдунщик');
    });

    it('Проверка счетчиков и ссылок', function() {
        const WEATHER_URL = 'https://yandex.ru/pogoda/moscow';

        return this.browser
            .yaCheckSnippet(PO.weather, {
                title: {
                    url: {
                        href: WEATHER_URL,
                        ignore: ['protocol']
                    },
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="weather"]/title'
                    }
                },
                greenurl: [{
                    url: {
                        href: WEATHER_URL,
                        ignore: ['protocol']
                    },
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="weather"]/path/urlnav'
                    }
                }]
            })
            .yaCheckLink(PO.weather.serviceLink()).then(url => this.browser
                .yaCheckURL(url, WEATHER_URL, 'Ссылка «Подробный прогноз» сломана', { skipProtocol: true }))
            .yaCheckBaobabCounter(PO.weather.serviceLink(), {
                path: '/$page/$main/$result[@wizard_name="weather"]/link'
            });
    });

    it('Проверка прогнозов', function() {
        return this.browser
            .yaVisibleCount(PO.weather.carousel.tiles())
            .then(count => assert.equal(count, 2, 'Должно быть видно 2 прогноза в карусели'))
            .yaVisibleCount(PO.weather.weekDays())
            .then(count => assert.equal(count, 3, 'Должно быть видно 3 прогноза по дням'));
    });
});
