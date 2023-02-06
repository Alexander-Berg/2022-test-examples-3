'use strict';

const PO = require('./CompaniesFooter.page-object')('touch-phone');

specs({
    feature: 'Колдунщик 1Орг',
    type: 'Футер',
}, function() {
    it('Ссылки', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.footer.owner(),
            url: {
                href: 'https://yandex.ru/sprav/search',
                queryValidator: query => {
                    assert.isDefined(query.permalink);
                    assert.equal(query.from, 'unisearch_footer');
                    assert.equal(query.utm_source, 'unisearch_touch');
                    return true;
                },
            },
            baobab: {
                path: '/$page/$main/$result/composite/object-footer/my',
            },
            message: 'Сломана ссылка, ведущая на Справочник',
        });

        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.footer.sprav(),
            url: {
                href: 'https://yandex.ru/support/sprav/add-company/add-org.html',
            },
            baobab: {
                path: '/$page/$main/$result/composite/object-footer/sprav',
            },
        });
    });
});
