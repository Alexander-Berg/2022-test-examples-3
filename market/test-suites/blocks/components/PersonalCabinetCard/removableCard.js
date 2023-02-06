import {makeSuite, makeCase, mergeSuites} from 'ginny';

import Dialog from '@self/platform/spec/page-objects/components/Dialog';
import PersonalCabinetCard from '@self/platform/components/PersonalCabinetCard/__pageObject';
import PersonalCabinetZeroState from '@self/platform/components/PersonalCabinetZeroState/__pageObject';

/**
 * @param {PageObject.widgets.content.UserQuestions.components.SnippetHeadline} snippetHeadline
 */
export default makeSuite('Единственный сниппет личного кабинета, диалог удаления.', {
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                dialog: () => this.createPageObject(Dialog),
                card: () => this.createPageObject(PersonalCabinetCard),
                zeroState: () => this.createPageObject(PersonalCabinetZeroState),
            });

            await this.snippetHeadline.clickOptions();
            await this.browser.yaWaitForChangeValue({
                action: () => this.snippetHeadline.clickRemove(),
                valueGetter: () => this.dialog.isVisible(),
            });
        },
        'По умолчанию': {
            'кнопка «Отменить» диалога удаления закрывает диалог, не удаляя сниппет': makeCase({
                id: 'marketfront-3853',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.dialog.clickCancelButton(),
                        valueGetter: () => this.dialog.isVisible(),
                    });

                    await this.card.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет не удалён');
                },
            }),
            '«крестик» окна диалога удаления закрывает диалог, не удаляя сниппет': makeCase({
                id: 'marketfront-3854',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.dialog.clickCloseButton(),
                        valueGetter: () => this.dialog.isVisible(),
                    });

                    await this.card.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет не удалён');
                },
            }),
            'клик вне окна диалога удаления закрывает диалог, не удаляя сниппет': makeCase({
                id: 'marketfront-3855',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.dialog.clickOutsideContent(),
                        valueGetter: () => this.dialog.isVisible(),
                    });

                    await this.card.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет не удалён');
                },
            }),
            'кнопка «Удалить» диалога удаления закрывает диалог, и удаляет сниппет': makeCase({
                id: 'marketfront-3856',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.dialog.clickSubmitButton(),
                        valueGetter: () => this.dialog.isVisible(),
                    });

                    await this.card.isVisible()
                        .should.eventually.be.equal(false, 'Сниппет удален');
                    return this.zeroState.isVisible()
                        .should.eventually.be.equal(true, 'Zero стейт страницы отображается');
                },
            }),
        },
    }),
});
