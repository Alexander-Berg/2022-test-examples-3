import {makeSuite, makeCase} from 'ginny';
import Notification from '@self/root/src/components/Notification/__pageObject';

/**
 * @param {PageObject.Answer} answerSnippet
 * @param {PageObject.Dialog} dialog
 * @param {PageObject.AnswersList} answersList
 */
export default makeSuite('Сниппет ответа, диалог удаления', {
    story: {
        'По умолчанию': {
            'кнопка «Отменить» диалога удаления закрывает диалог, не удаляя ответ': makeCase({
                id: 'marketfront-2907',
                issue: 'MARKETVERSTKA-31299',
                feature: 'Удаление ответа',
                async test() {
                    await this.answerSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickCancelButton();
                    await this.dialog.waitForContentHidden();
                    const isAnswerSnippetExists = await this.answerSnippet.isExisting();
                    await this.expect(isAnswerSnippetExists).to.be.equal(true, 'Ответ не удалён');
                },
            }),
            '«крестик» окна диалога удаления закрывает диалог, не удаляя ответ': makeCase({
                id: 'marketfront-2908',
                issue: 'MARKETVERSTKA-31300',
                feature: 'Удаление ответа',
                async test() {
                    await this.answerSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickCloseButton();
                    await this.dialog.waitForContentHidden();
                    const isAnswerSnippetExists = await this.answerSnippet.isExisting();
                    await this.expect(isAnswerSnippetExists).to.be.equal(true, 'Ответ не удалён');
                },
            }),
            'клик вне окна диалога удаления закрывает диалог, не удаляя ответ': makeCase({
                id: 'marketfront-2909',
                issue: 'MARKETVERSTKA-31301',
                feature: 'Удаление ответа',
                async test() {
                    await this.answerSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickOutsideContent();
                    await this.dialog.waitForContentHidden();
                    const isAnswerSnippetExists = await this.answerSnippet.isExisting();
                    await this.expect(isAnswerSnippetExists).to.be.equal(true, 'Ответ не удалён');
                },
            }),
        },
        'При подтверждении удаления': {
            'после удаления ответа отображается нотификация «Ответ удалён»': makeCase({
                id: 'marketfront-2906',
                issue: 'MARKETVERSTKA-31298',
                feature: 'Удаление ответа',
                async test() {
                    this.setPageObjects({
                        notification: () => this.createPageObject(Notification),
                    });

                    await this.answerSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickSubmitButton();
                    await this.dialog.waitForContentHidden();
                    await this.notification
                        .getText()
                        .should.eventually.be.equal('Ответ удалён');
                },
            }),
            'после удаления ответ удаляется из списка': makeCase({
                id: 'marketfront-2906',
                issue: 'MARKETVERSTKA-31298',
                feature: 'Удаление ответа',
                async test() {
                    const answersCountBefore = await this.answersList.answersCount;

                    await this.answerSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickSubmitButton();
                    await this.dialog.waitForContentHidden();

                    const answersCountAfter = await this.answersList.answersCount;

                    await this.expect(answersCountBefore - 1).to.be.equal(answersCountAfter, 'Ответ удалён');
                },
            }),
            'после удаления количество ответов в заголовке уменьшается на 1': makeCase({
                id: 'marketfront-2917',
                issue: 'MARKETVERSTKA-31266',
                feature: 'Структура страницы',
                async test() {
                    const countBefore = await this.answersList.getAnswersCountFromHeader();

                    await this.expect(countBefore).to.be.equal(
                        this.params.answersCount,
                        'На странице ожидаемое количество ответов'
                    );

                    await this.answerSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickSubmitButton();
                    await this.dialog.waitForContentHidden();

                    const countAfter = await this.answersList.getAnswersCountFromHeader();

                    await this.expect(countAfter).to.be.equal(
                        (countBefore - 1),
                        'На странице количество ответов уменьшилось на 1'
                    );
                },
            }),
        },
    },
});
