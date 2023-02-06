'use strict';

const PO = require('./CovidMap.page-object').touchPhone;

specs('Карта коронавируса', function() {
    it('Основные проверки', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'коронавирус карта россии',
                data_filter: 'special/event',
            },
            PO.covidMap(),
        );

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result[@wizard_name="special/event" and @subtype="virus_map"]',
        });

        await this.browser.assertView('covid-map', PO.covidMap());

        await this.browser.yaCheckLink2({
            selector: PO.covidMap.organic.title.link(),
            url: { href: 'https://yandex.ru/web-maps/covid19', ignore: ['query'] },
            baobab: { path: '/$page/$main/$result/title' },
        });
        await this.browser.yaCheckLink2({
            selector: PO.covidMap.map.link(),
            url: { href: 'https://yandex.ru/web-maps/covid19', ignore: ['query'] },
            baobab: { path: '/$page/$main/$result/map/map' },
        });
    });
});
