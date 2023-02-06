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
        'При потверждение удаления': {
            'отзыв удаляется.': makeCase({
                test() {
                    return this.browser.waitForVisible(ShopReview.deleteLink)
                        .then(() => this.productReviewItem.clickDeleteLink())
                        .then(() => this.confirmDeleteDialog.waitForVisible())
                        .then(() => this.confirmDeleteDialog.clickSubmitLink())
                        .then(() => this.allure.runStep('После нажатия кнопки "Да, удалить"', () => this.browser
                            .waitForVisible(ShopReview.deleteLink, this.browser.options.waitforTimeout, true)
                            .catch(() => false)))
                        .then(() => this.browser.yaReactPageReload())
                        .then(() => this.allure.runStep(
                            'После обновления страницы',
                            () => this.productReviewItem.isExisting()
                                .should.eventually.equal(false, 'Отзыва нет на странице')
                        ));
                },
            }),
        },
    },
});
