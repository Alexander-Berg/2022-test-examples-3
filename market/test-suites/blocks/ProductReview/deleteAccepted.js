import {makeSuite, makeCase} from 'ginny';
import ConfirmReviewRemoveDialog from '@self/platform/spec/page-objects/components/ProductReview/Footer/RemoveReviewConfirmDialog';
import ProductReviewsList from '@self/platform/spec/page-objects/components/ProductReviewsList';


/**
 * Тесты на компонент productReview
 *
 * @param {PageObject.ProductReview} productReview
 */
export default makeSuite('Промодерированный отзыв пользователя.', {
    feature: 'Удаление отзыва',
    story: {
        beforeEach() {
            this.setPageObjects({
                confirmReviewRemoveDialog: () => this.createPageObject(ConfirmReviewRemoveDialog),
                productReviewsList: () => this.createPageObject(ProductReviewsList),
            });
        },
        'При потверждение удаления': {
            'отзыв удаляется.': makeCase({
                async test() {
                    await this.productReviewsList.getReviewsCount().should.eventually.equal(
                        2,
                        'До удаления отзыва на странице 2 отзыва'
                    );
                    await this.productReview.clickRemoveReviewButton();
                    await this.confirmReviewRemoveDialog.clickSubmitButton();
                    await this.confirmReviewRemoveDialog.isNotVisible();
                    await this.productReviewsList.getReviewsCount().should.eventually.equal(
                        1,
                        'После удаления отзыва остается 1 отзыв'
                    );
                    await this.browser.yaPageReload(5000, ['state']);
                    return this.productReviewsList.getReviewsCount().should.eventually.equal(
                        1,
                        'И после обновления страницы остается 1 отзыв'
                    );
                },
            }),
        },
    },
});
