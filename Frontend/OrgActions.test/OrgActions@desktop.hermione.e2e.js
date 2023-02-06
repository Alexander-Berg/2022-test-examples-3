'use strict';

const PO = require('./OrgActions.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    experiment: 'Кнопки',
}, function() {
    it('Наличие элементов в карточке в правой колонке', async function() {
        const fallbackUrl = '/search/touch?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result/composite/tabs/about/actions/site',
            PO.oneOrg.buttons(),
            fallbackUrl,
        );
    });

    it('Наличие элементов в карточке в центре', async function() {
        const fallbackUrl = '/search/touch?text=кафе breadway екатеринбург&oid=b:114337144368';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/actions/site',
            PO.oneOrgLeft.buttons(),
            fallbackUrl,
        );
    });
});
