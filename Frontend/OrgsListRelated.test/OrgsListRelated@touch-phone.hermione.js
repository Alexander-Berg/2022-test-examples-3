'use strict';

const PO = require('./OrgsListRelated.page-object').touchPhone;

specs('Оргмн', function() {
    it('Похожие запросы под колдунщиком', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе в москве',
            exp_flags: 'GEO_orgmn_discovery=after',
            srcrwr: 'GEOV:rosetta.search.yandex.net:9904:1s',
            srcparams: 'GEOV:rearr=scheme_Local/Geo/DiscoveryAspects/Enabled=true',
            data_filter: { values: ['companies', 'generic'], operation: 'OR' },
        }, PO.orgRelated());
        await browser.yaAssertViewExtended('after', PO.orgRelated());

        let relatedText = await browser.getText(PO.orgRelated.firstItem());

        relatedText = relatedText.replace('\n', ' ') + ' в Москве';

        await browser.yaWaitUntilSerpReloaded(
            () => browser.yaCheckLink2({
                selector: PO.orgRelated.firstItem.link(),
                target: '_self',
                url: {
                    href: 'https://yandex.ru/search/touch/?noredirect=1&text=...&lr=213&noreask=1&serp-reload-from=companies',
                    queryValidator: query => {
                        assert.equal(query.text, relatedText, 'Ошибка в параметре text');
                        assert.equal(query.noreask, '1', 'Ошибка в параметре noreask');
                        assert.equal(query['serp-reload-from'], 'companies', 'Ошибка в параметре serp-reload-from');

                        return true;
                    },
                    ignore: ['hostname'],
                },
                baobab: { path: '/$page/$main/$result/composite/org-related/scroller/link[@id]' },
                message: 'Ошибка в первой ссылке',
            }),
        );
    });

    hermione.only.in(['searchapp-phone'], 'Проверка актуальна только для ПП');
    it('Похожие запросы под колдунщиком - ПП', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе в москве',
            exp_flags: ['GEO_orgmn_discovery=after', 'fake_yandex_apps_api=1'],
            srcparams: 'GEOV:rearr=scheme_Local/Geo/DiscoveryAspects/Enabled=true',
            data_filter: 'companies',
        }, PO.orgRelated());
        await browser.yaCheckApiOpenRelatedQuery(PO.orgRelated.firstItem.link());
    });
});
