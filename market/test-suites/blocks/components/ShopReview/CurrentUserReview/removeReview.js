import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CurrentUserReview} currentUserReview
 * @param {PageObject.RemovePromptDialog} removePromptDialog
 */
export default makeSuite('Блок «Ваш отзыв». Удаление', {
    feature: 'Рейтинг магазина',
    story: {
        'Кнопка «Удалить».': {
            'При клике открывается диалог удаления, в котором отрабатывает кнопка удаления отзыва': makeCase({
                id: 'm-touch-2366',
                issue: 'MOBMARKET-9583',
                async test() {
                    await this.header.openReviewMenu();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });

                    await this.removePromptDialog.clickSubmitButton();
                    await this.removePromptDialog.waitForContentHidden();

                    await this.currentUserReview.waitForHidden();
                    await this.expect(this.currentUserReview.isVisible())
                        .to.be.equal(false, 'Блок «Ваш отзыв» не отображается');
                },
            }),
        },
    },
});
