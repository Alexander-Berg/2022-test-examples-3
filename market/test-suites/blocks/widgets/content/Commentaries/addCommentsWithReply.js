import {makeCase, makeSuite} from 'ginny';
import SmallForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import EditForm from '@self/platform/spec/page-objects/components/Comment/EditForm';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';

/**
 * @param {PageObject.components.Comment.Toolbar} firstLevelToolbar
 * @param {PageObject.components.Comment.Toolbar} secondLevelToolbar
 * @param {PageObject.components.Comment.Snippet} firstLevelSnippet
 * @param {PageObject.components.Comment.Snippet} secondLevelSnippet
 * @params {string} replyTo ожидаемое обращение к автору родительского комментария
 * @params {string} text1 текст в первом сниппете
 * @params {string} text3 текст в третьем сниппете
 */
export default makeSuite('Добавление ответа через кнопку в тулбаре', {
    params: {
        replyTo: 'Обращение к автору родительского комментария',
    },
    story: {
        async beforeEach() {
            await this.setPageObjects({
                smallForm: () => this.createPageObject(SmallForm, this.firstLevelSnippet),
                editForm: () => this.createPageObject(EditForm),
                controls: () => this.createPageObject(Controls),
            });
            await this.firstLevelToolbar.isVisible();
            await this.firstLevelToolbar.clickAnswerControl();
            await this.smallForm.isVisible()
                .should.eventually.to.be.equal(true, 'Форма ответа на комментарий появилась');
        },
        'При нажатии на кнопку Комментировать': {
            'открывается форма комментария': {
                'и в форме проставляется обращение': makeCase({
                    id: 'm-touch-2773',
                    issue: 'MOBMARKET-12937',
                    async test() {
                        await this.smallForm.getText()
                            .should.eventually.to.be.equal(this.params.replyTo, 'В форму проставилось обращение');
                    },
                }),
            },
        },
        'При редактирование вновь созданного коммента с обращением': {
            'кнопка очищения формы': {
                'оставляет в форме обращение': makeCase({
                    id: 'm-touch-2939',
                    issue: 'MOBMARKET-12937',
                    async test() {
                        const replyTo = this.params.replyTo;
                        const text = 'some text';
                        await this.smallForm.getText()
                            .should.eventually.to.be.equal(replyTo, 'В форму проставилось обращение');
                        await this.smallForm.editText(text);
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.smallForm.clickSendButton(),
                            valueGetter: () => this.secondLevelSnippet.isVisible(),
                        });
                        await this.secondLevelSnippet.getVisibleSnippetText()
                            .should.eventually.to.equal(`${replyTo}${text}`, 'Текст комментария корректный');
                        await this.secondLevelSnippet.moreActionsClick();
                        await this.controls.isVisible();
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.controls.clickEditButton(),
                            valueGetter: () => this.editForm.isVisible(),
                        });
                        await this.editForm.isClearTextFieldButtonVisible()
                            .should.eventually.to.be.equal(true, 'Кнопка очистки формы редактирования отображается');
                        await this.editForm.clickClearTextFieldButton();
                        await this.editForm.getText()
                            .should.eventually.to.be.equal(this.params.replyTo, 'Форма очистилась до обращения');
                    },
                }),
            },
        },
    },
});
