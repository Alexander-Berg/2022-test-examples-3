'use strict';

const PO = require('./OrgReviewsPreview.page-object')('touch-phone');

specs({
    feature: 'Отзывы на Серпе',
    type: 'Превью отзывов в колдунщике организации',
}, function() {
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());
        await browser.assertView('plain', PO.oneOrg.reviewsPreview());
    });

    it('Сортировка отзывов', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());

        await this.browser.yaWaitForVisible(PO.oneOrg.reviewsPreview.reviewsRankingSelector());

        const firstReviewText = await this.browser.getText(PO.oneOrg.reviewsPreview.list.firstReview.visibleText());

        await this.browser.yaCheckBaobabCounter(
            PO.oneOrg.reviewsPreview.reviewsRankingSelector.negative(),
            {
                path: '/$page/$main/$result/composite/reviews/ranking-selector/item',
                behaviour: { type: 'dynamic' },
                attrs: { ranking: 'by_rating_asc' },
            },
        );

        await this.browser.yaWaitUntil('Отзывы не перезагрузились', async function() {
            const text = await this.getText(PO.oneOrg.reviewsPreview.list.firstReview.visibleText());

            return text !== firstReviewText;
        });
    });

    it('Сортировка прокидывается в попап', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
        }, PO.oneOrg());

        await this.browser.yaWaitForVisible(PO.oneOrg.reviewsPreview.reviewsRankingSelector());

        await this.browser.yaCheckBaobabCounter(
            PO.oneOrg.reviewsPreview.reviewsRankingSelector.positive(),
            {
                path: '/$page/$main/$result/composite/reviews/ranking-selector/item',
                behaviour: { type: 'dynamic' },
                attrs: { ranking: 'by_rating_desc' },
            },
        );

        await this.browser.click(PO.oneOrg.reviewsPreview.readAll());
        await this.browser.yaWaitForVisible(PO.overlay.reviews.sortingSelect());

        const text = await this.browser.getText(PO.overlay.reviews.sortingSelect.text());

        assert.equal(text, 'Сначала положительные', 'Тип сортировки в попапе не изменился');
    });
});
