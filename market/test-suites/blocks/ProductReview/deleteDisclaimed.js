import {makeSuite, makeCase} from 'ginny';
import ProductReview from '@self/platform/spec/page-objects/components/ProductReview';
import ConfirmReviewRemoveDialog from '@self/platform/spec/page-objects/components/ProductReview/Footer/RemoveReviewConfirmDialog';

/**
 * Тесты на компонент productReview.
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Промодерированный отзыв пользователя.', {
    feature: 'Удаление отзыва',
    story: {
        beforeEach() {
            this.setPageObjects({
                confirmReviewRemoveDialog: () => this.createPageObject(ConfirmReviewRemoveDialog),
            });
        },
        'При отмене удаления': {
            'отзыв остается.': makeCase({
                test() {
                    return this.productReview.clickRemoveReviewButton()
                        .then(() => this.confirmReviewRemoveDialog.waitForVisible())
                        .then(() => this.confirmReviewRemoveDialog.clickCancelButton())
                        .then(() => this.allure.runStep(
                            'После нажатия кнопки "Нет, не удалять"',
                            () => this.productReview
                                .removeReviewButton
                                .isVisible()
                                .should.eventually.equal(true, 'Отзыв остался на странице')))
                        .then(() => this.productReview.isVisible())
                        .then(() => this.browser.yaPageReload(5000, ['state']))
                        .then(() => this.browser.waitForVisible(ProductReview.root))
                        .then(() => this.allure.runStep('После обновления страницы', () => this.productReview
                            .removeReviewButton
                            .isVisible()
                            .should.eventually.equal(true, 'Отзыв есть на странице')
                        ));
                },
            }),
        },
    },
});
