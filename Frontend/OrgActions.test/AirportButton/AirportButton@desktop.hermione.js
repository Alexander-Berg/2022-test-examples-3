'use strict';

const PO = require('../OrgActions.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Расписание рейсов',
}, () => {
    hermione.also.in(['firefox', 'ipad']);
    it('Основные проверки', async function() {
        await this.browser.yaOpenSerp({
            text: 'аэропорт внуково',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.assertView('plain', PO.oneOrg.buttons());

        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.buttons.airport(),
            baobab: {
                path: '/$page/$parallel/$result/composite/tabs/about/actions/airport[@action = "cta"]',
            },
            url: {
                href: {
                    hostname: 'rasp.yandex.ru',
                    pathname: '/station/9600215/',
                },
                ignore: ['protocol', 'query'],
            },
        });
    });
});
