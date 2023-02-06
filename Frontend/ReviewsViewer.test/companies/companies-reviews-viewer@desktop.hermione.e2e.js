'use strict';

const PO = require('./companies.page-object/index@desktop');

specs({
    feature: 'Отзывы на Серпе',
    type: 'Просмотрщик отзывов организации',
}, function() {
    it('Наличие отзывов', async function() {
        const { browser } = this;
        const fallbackUrl = [
            '/search/?text=ква ква парк&lr=213',
            '/search/?text=erwin рекамореокеан кутузовский проспект&lr=213',
            '/search/?text=жк бунинские луга&lr=213',
            '/search/?text=тц европейский площадь киевского вокзала&lr=213',
            '/search/?text=кафе пушкин москва&lr=213'];

        await browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="company"]/composite/tabs/about/reviews-loader/reviews/title',
            PO.oneOrg.reviewsPreview(),
            fallbackUrl,
        );

        await browser.click(PO.oneOrg.reviewsPreview.title.link());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
    });
});
