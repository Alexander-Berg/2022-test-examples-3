import {makeCase, makeSuite} from 'ginny';
import SmallForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';

/**
 * @param {PageObject.components.Comment.Snippet} secondLevelSnippet сниппет 1ого комментария 2ого уровня
 * @params {string} replyTo ожидаемое обращение к автору родительского комментария
 */
export default makeSuite('Добавление еще одного комментария с обращением.', {
    params: {
        replyTo: 'Обращение к автору родительского комментария',
    },
    story: {
        async beforeEach() {
            await this.setPageObjects({
                smallForm: () => this.createPageObject(SmallForm),
                notification: () => this.createPageObject(Notification),
                controls: () => this.createPageObject(Controls),
            });
            await this.controls.isVisible();
            await this.browser.yaWaitForChangeValue({
                action: () => this.controls.clickAnswerButton(),
                valueGetter: () => this.smallForm.isVisible(),
            });
        },
        'При нажатии кнопки очищения формы': {
            'в форме остается обращение': makeCase({
                id: 'm-touch-2937',
                issue: 'MOBMARKET-12937',
                async test() {
                    await this.smallForm.isClearTextFieldButtonVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка очистки формы отображается');
                    await this.smallForm.clickClearTextFieldButton();
                    await this.smallForm.getText()
                        .should.eventually.to.be.equal(this.params.replyTo, 'Форма очистилась до обращения');
                },
            }),
        },
        'При добавление комментария': {
            'без обращения': {
                'обращение не подставляется в сниппете': makeCase({
                    id: 'm-touch-2938',
                    issue: 'MOBMARKET-12937',
                    async test() {
                        await this.smallForm.setText('!');
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.smallForm.clickSendButton(),
                            valueGetter: () => this.secondLevelSnippet.isVisible(),
                        });
                        await this.secondLevelSnippet.getVisibleSnippetText()
                            .should.eventually.to.equal('!', 'Текст комментария корректный');
                    },
                }),
            },
            'добавляя текст и не затрагивая обращение': {
                'в сниппете отображается текст с обращением': makeCase({
                    id: 'marketfront-2775',
                    issue: 'MOBMARKET-12937',
                    async test() {
                        const {replyTo} = this.params;
                        const text = 'some text';
                        await this.smallForm.editText(text);
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.smallForm.clickSendButton(),
                            valueGetter: () => this.secondLevelSnippet.isVisible(),
                        });
                        await this.secondLevelSnippet.getVisibleSnippetText()
                            .should.eventually.to.equal(`${replyTo}${text}`, 'Текст комментария корректный');
                    },
                }),
            },
        },
    },
});
