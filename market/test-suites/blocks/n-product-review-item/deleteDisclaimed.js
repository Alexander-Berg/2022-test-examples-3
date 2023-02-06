import {makeSuite, makeCase} from 'ginny';

import ShopReview from '@self/platform/components/ShopReview/Review/__pageObject';
import ConfirmDeleteDialog from '@self/platform/components/ShopReview/ReviewFooter/__pageObject/ConfirmDeleteDialog';

/**
 * Тесты на блок n-review-form-shop.
 *
 * @param {PageObject.ShopReview} productReviewItem
 */
export default makeSuite('Промодерированный отзыв пользователя.', {
    feature: 'Удаление отзыва',
    story: {
        beforeEach() {
            this.setPageObjects({
                confirmDeleteDialog: () => this.createPageObject(ConfirmDeleteDialog),
            });
        },
        'При отмене удаления': {
            'отзыв остается.': makeCase({
                test() {
                    return this.browser.waitForVisible(ShopReview.deleteLink)
                        .then(() => this.productReviewItem.clickDeleteLink())
                        .then(() => this.confirmDeleteDialog.waitForVisible())
                        .then(() => this.confirmDeleteDialog.clickResetLink())
                        .then(() => this.allure.runStep(
                            'После нажатия кнопки "Нет, не удалять"',
                            () => this.productReviewItem
                                .deleteLink
                                .isVisible()
                                .should.eventually.equal(true, 'Отзыв остался на странице')))
                        .then(() => this.productReviewItem.isVisible())
                        .then(() => this.browser.yaReactPageReload())
                        .then(() => this.browser.waitForVisible(ShopReview.root))
                        .then(() => this.allure.runStep('После обновления страницы', () => this.productReviewItem
                            .deleteLink
                            .isVisible()
                            .should.eventually.equal(true, 'Отзыв есть на странице')
                        ));
                },
            }),
        },
    },
});
