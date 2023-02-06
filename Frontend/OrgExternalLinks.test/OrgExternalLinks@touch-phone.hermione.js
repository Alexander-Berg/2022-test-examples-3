'use strict';

const PO = require('./OrgExternalLinks.page-object');

specs({
    feature: 'Одна организация',
    type: 'Ссылки на внешние сервисы',
}, function() {
    hermione.only.notIn(['chrome-phone'], 'проблемы с дампами метрик');
    it('Без данных', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'кафе пушкин',
                exp_flags: ['GEO_1org_externals', 'GEO_externals_data=2'],
                data_filter: 'companies',
            },
            PO.OrgExternalLinks(),
        );

        await this.browser.assertView('plain', PO.OrgExternalLinks());

        await this.browser.yaCheckLink2({
            selector: PO.OrgExternalLinks.secondItem(),
            url: {
                href: 'https://yandex.ru/maps',
            },
            target: '_blank',
            baobab: {
                path: '/$page/$main/$result/composite/externals[@externalId@entity="organization"]/yandex-maps',
            },
        });
    });

    it('С данными', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'la belle studio стачек',
                exp_flags: ['GEO_1org_externals'],
                data_filter: 'companies',
                srcparams: 'GEOV:experimental=add_snippet=flinks/1.x',
            },
            PO.OrgExternalLinks(),
        );

        await this.browser.yaCheckLink2({
            selector: PO.OrgExternalLinks.item(),
            url: {
                href: 'https://yandex.ru/maps/org/1051714908/',
                ignore: ['query'],
            },
            target: '_blank',
            baobab: {
                path: '/$page/$main/$result/composite/externals[@externalId@entity="organization"]/yandex-maps',
            },
        });
    });
});
