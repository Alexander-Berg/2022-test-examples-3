import {makeCase, makeSuite} from 'ginny';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Notification from '@self/root/src/components/Notification/__pageObject';

/**
 * @param {PageObject.UserQuestions} userQuestions
 * @param {PageObject.CabinetQuestionSnippet} questionSnippet
 * @param {PageObject.QuestionHeader} questionHeader
 */
export default makeSuite('Единственный вопрос у которого нет ответов.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                notification: () => this.createPageObject(Notification),
            });
            await this.browser.yaWaitForChangeValue({
                action: () => this.questionHeader.clickDeleteButton(),
                valueGetter: () => this.removePromptDialog.isVisible(),
            });
        },
        'Кнопка удаления ответа': {
            'при клике': {
                'При выборе отмены в диалоговом окне': {
                    'окно закрывается, вопрос остается': makeCase({
                        id: 'm-touch-3116',
                        issue: 'MARKETFRONT-6439',
                        async test() {
                            await this.expect(this.removePromptDialog.isVisible())
                                .to.equal(true, 'Диалоговое окно появилось');
                            await this.removePromptDialog.clickCloseButton();
                            await this.removePromptDialog.waitForContentHidden();
                            await this.questionSnippet.isVisible();
                        },
                    }),
                },
                'При клике вне диалогового окна': {
                    'окно закрывается, вопрос остается': makeCase({
                        id: 'm-touch-3117',
                        issue: 'MARKETFRONT-6439',
                        async test() {
                            await this.expect(this.removePromptDialog.isVisible())
                                .to.equal(true, 'Диалоговое окно появилось');
                            await this.removePromptDialog.clickOutsideContent();
                            await this.removePromptDialog.waitForContentHidden();
                            await this.questionSnippet.isVisible();
                        },
                    }),
                },
                'При подтверждении удаления': {
                    'окно закрывается, вопрос удаляется': makeCase({
                        id: 'm-touch-3118',
                        issue: 'MARKETFRONT-6439',
                        async test() {
                            await this.expect(this.removePromptDialog.isVisible())
                                .to.equal(true, 'Диалоговое окно появилось');
                            await this.removePromptDialog.clickSubmitButton();
                            await this.removePromptDialog.waitForContentHidden();
                            await this.notification.waitForText('Вопрос удалён');
                            await this.userQuestions.userQuestionsCount
                                .should.eventually.to.be.equal(0, 'Список вопросов пуст');
                            await this.userQuestions.isZeroStateVisible()
                                .should.eventually.to.be.equal(true, 'Блок "Задайте вопрос" виден');
                        },
                    }),
                },
            },
        },
    },
});
