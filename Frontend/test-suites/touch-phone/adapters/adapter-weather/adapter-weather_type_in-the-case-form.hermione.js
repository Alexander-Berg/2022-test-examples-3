'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Колдунщик погоды / по запросу в падежной форме', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=погода в питере&lr=213')
            .yaWaitForVisible(PO.weather(), 'Не появился колдунщик');
    });

    it('Проверка счетчиков и ссылок', function() {
        const WEATHER_URL = 'https://yandex.ru/pogoda/saint-petersburg';

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
            .yaCheckNativeScroll(PO.weather.carousel(), PO.weather.carousel.firstTile())
            .yaVisibleCount(PO.weather.weekDays())
            .then(count => assert.equal(count, 3, 'Должно быть видно 3 прогноза по дням'));
    });
});
