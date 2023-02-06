'use strict';

const PO = require('./OrgExternalLinks.page-object');

specs({
    feature: 'Одна организация',
    type: 'Ссылки на внешние сервисы',
}, function() {
    it('Без данных', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'кафе пушкин',
                exp_flags: ['GEO_1org_external_links', 'GEO_externals_data=2'],
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
                path: '/$page/$parallel/$result/composite/tabs/about/externals[@externalId@entity="organization"]/yandex-maps',
            },
        });
    });

    it('С данными', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'la belle studio стачек',
                exp_flags: ['GEO_1org_external_links'],
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
                path: '/$page/$parallel/$result/composite/tabs/about/externals[@externalId@entity="organization"]/yandex-maps',
            },
        });
    });
});
