import {makeCase, makeSuite} from 'ginny';
import EditForm from '@self/platform/spec/page-objects/components/Comment/EditForm';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';

/**
 * @param {PageObject.components.Comment.Toolbar} secondLevelChildrenToolbar1 тулбар 1ого комментария 2ого уровня
 * @param {PageObject.components.Comment.Toolbar} secondLevelChildrenToolbar2 тулбар 2ого комментария 2ого уровня
 * @param {PageObject.components.Comment.Snippet} secondLevelSnippet1 сниппет 1ого комментария 2ого уровня
 * @params {string} replyTo ожидаемое обращение к автору родительского комментария
 * @params {string} text1 текст в первом сниппете
 * @params {string} text3 текст в третьем сниппете
 */
export default makeSuite('Комментарий с обращением.', {
    params: {
        replyTo: 'Обращение к автору родительского комментария',
        text1: 'Текст первого сниппета второго уровня',
        text2: 'Текст второго сниппета второго уровня',
        text3: 'Текст третьего сниппета второго уровня',
    },
    story: {
        async beforeEach() {
            await this.setPageObjects({
                editForm: () => this.createPageObject(EditForm),
                notification: () => this.createPageObject(Notification),
                controls: () => this.createPageObject(Controls),
            });
            await this.controls.isVisible();
            await this.controls.clickEditButton();
            await this.secondLevelChildrenToolbar1.isVisible()
                .should.eventually.to.be.equal(false, 'Первый сниппет второго уровня больше не отображается');
        },
        'При нажатии кнопки очищения формы': {
            'в форме остается обращение': makeCase({
                id: 'm-touch-2909',
                issue: 'MOBMARKET-11926',
                async test() {
                    await this.editForm.isClearTextFieldButtonVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка очистки формы редактирования отображается');
                    await this.editForm.clickClearTextFieldButton();
                    await this.editForm.getText()
                        .should.eventually.to.be.equal(this.params.replyTo, 'Форма очистилась до обращения');
                },
            }),
        },
        'При изменение комментария': {
            'на текст без обращения': {
                'обращение не подставляется в сниппете': makeCase({
                    id: 'm-touch-2903',
                    issue: 'MOBMARKET-11926',
                    async test() {
                        await this.editForm.setText('!');
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.editForm.clickSendButton(),
                            valueGetter: () => this.secondLevelChildrenToolbar1.isVisible(),
                        });
                        await this.secondLevelSnippet1.getVisibleSnippetText()
                            .should.eventually.to.equal('!', 'Текст комментария корректный');
                    },
                }),
            },
            'изменяя только текст и не затрагивая обращение': {
                'в сниппете отображается текст с обращением': makeCase({
                    id: 'marketfront-3622',
                    issue: 'MOBMARKET-11926',
                    async test() {
                        const {replyTo, text1} = this.params;
                        await this.editForm.editText('!');
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.editForm.clickSendButton(),
                            valueGetter: () => this.secondLevelChildrenToolbar1.isVisible(),
                        });
                        await this.secondLevelSnippet1.getVisibleSnippetText()
                            .should.eventually.to.equal(`${replyTo}${text1}!`, 'Текст комментария корректный');
                    },
                }),
            },
            'нажимаем на кнопку тулбара другого комментария': {
                'форма с редактированием заменяется на сниппет': makeCase({
                    id: 'm-touch-2902',
                    issue: 'MOBMARKET-11926',
                    async test() {
                        const {replyTo, text1} = this.params;
                        await this.editForm.editText('!');
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.secondLevelChildrenToolbar2.moreActionsClick(),
                            valueGetter: () => this.secondLevelChildrenToolbar1.isVisible(),
                        });
                        await this.secondLevelSnippet1.getVisibleSnippetText()
                            .should.eventually.to.equal(`${replyTo}${text1}`, 'Текст комментария корректный');
                    },
                }),
            },
            'меняем текст на такой же, как у другого комментария с обращением': {
                'получаем сообщение об ошибке при попытке сохранить': makeCase({
                    id: 'm-touch-2905',
                    issue: 'MOBMARKET-11926',
                    async test() {
                        const {text2} = this.params;
                        await this.editForm.setText(text2);
                        await this.editForm.clickSendButton();
                        await this.notification.waitForText('Ваш комментарий уже принят');
                    },
                }),
            },
            'меняем текст на такой же, как у другого комментария без обращения': {
                'получаем сообщение об ошибке при попытке сохранить': makeCase({
                    id: 'm-touch-2906',
                    issue: 'MOBMARKET-11926',
                    async test() {
                        const {text3} = this.params;
                        await this.editForm.isClearTextFieldButtonVisible()
                            .should.eventually.to.be.equal(true, 'Кнопка очистки формы редактирования отображается');
                        await this.editForm.setText(text3);
                        await this.editForm.clickSendButton();
                        await this.notification.waitForText('Ваш комментарий уже принят');
                    },
                }),
            },
        },
    },
});
