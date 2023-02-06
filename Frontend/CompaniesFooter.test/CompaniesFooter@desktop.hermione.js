'use strict';

const POSimilar = require('../../SimilarCompanies/SimilarCompanies.test/SimilarCompanies.page-object/index@desktop');
const PO = require('./CompaniesFooter.page-object')('desktop');

specs({
    feature: 'Колдунщик 1Орг',
    type: 'Футер',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.oneOrg());
    });

    it('В попапе', async function() {
        await this.browser.click(POSimilar.oneOrg.similarCompanies.scroller.thirdItem());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'попап с 1орг не открылся');
        await this.browser.yaWaitForVisible(PO.popup.oneOrg.footer());

        await this.browser.yaScroll(PO.popup.oneOrg.footer());
        await this.browser.assertView('popup', PO.popup.oneOrg.footer(), {
            invisibleElements: `body > *:not(${PO.popup()})`,
        });
    });

    it('Ссылки', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.oneOrg.footer.sprav(),
            url: {
                href: 'https://yandex.ru/support/sprav/add-company/add-org.html',
            },
            baobab: {
                path: '$page/$parallel/$result/composite/object-footer/sprav',
            },
        });
    });
});
