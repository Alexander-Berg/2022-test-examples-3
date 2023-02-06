'use strict';

const reviewsPreviewPO = require('../../../Companies/Companies.features/OrgReviewsPreview/OrgReviewsPreview.test/OrgReviewsPreview.page-object/index@touch-phone');
const PO = require('./companies.page-object/index@touch-phone');

specs({
    feature: 'Отзывы на Серпе',
    type: 'Просмотрщик отзывов организации',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            exp_flags: 'GEO_1org_new_revews_preview=1;GEO_1org_reviews_swiper=1',
        }, reviewsPreviewPO.oneOrg());
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.click(reviewsPreviewPO.oneOrg.reviewsPreview.title.link());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        await browser.yaWaitForVisible(PO.reviewViewerModal.reviewItem(), 'Не загрузились отзывы в просмотрщике');
        await browser.assertView('plain', PO.reviewViewerModal());
    });

    describe('Точки входа', async function() {
        it('Заголовок отзывов', async function() {
            const { browser } = this;

            await browser.click(reviewsPreviewPO.oneOrg.reviewsPreview.title.link());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Таб "Отзывы"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.reviews());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Читать все отзывы', async function() {
            const { browser } = this;

            await browser.click(reviewsPreviewPO.oneOrg.reviewsPreview.readAll());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Признаки доверия', async function() {
            const { browser } = this;

            await browser.click(reviewsPreviewPO.oneOrg.reviewsPreview.aspectsList.firstCard());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });

        it('Отзывы', async function() {
            const { browser } = this;

            await browser.click(reviewsPreviewPO.oneOrg.reviewsPreview.list.firstReview());
            await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        });
    });

    it('Подскролл к активному отзыву', async function() {
        const { browser } = this;

        await browser.click(reviewsPreviewPO.oneOrg.reviewsPreview.list.firstReview.cut.more());
        await browser.yaWaitForVisible(PO.reviewViewerModal(), 'Просмотрщик не открылся');
        await browser.yaWaitForVisible(PO.reviewViewerModal.reviewItem(), 'Отзывы не загрузились');

        const value = await browser.execute(function(page) {
            const element = document.querySelector(page);

            return element.scrollTop;
        }, PO.reviewViewerModal.page());

        assert(value !== 0, 'Не произошёл подскролл к активному отзыву');
    });
});
