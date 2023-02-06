import {makeCase, makeSuite} from 'ginny';
import ComplaintForm from '@self/platform/spec/page-objects/components/ComplaintForm';
import ComplaintFormSubmitButton from '@self/platform/spec/page-objects/components/ComplaintForm/SubmitButton';
import ComplaintFormHeader from '@self/platform/spec/page-objects/components/ComplaintForm/Header';
import Notification from '@self/root/src/components/Notification/__pageObject';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';

/**
 * @param {PageObject.Comment.Snippet} commentSnippet
 * @param {PageObject.Comment.Controls} controls
 */
export default makeSuite('Блок комментария на статью, просматриваемый не автором коммента.', {
    story: {
        async beforeEach() {
            await this.setPageObjects({
                complaintForm: () => this.createPageObject(ComplaintForm),
                complaintFormSubmitButton: () => this.createPageObject(ComplaintFormSubmitButton),
                complaintFormHeader: () => this.createPageObject(ComplaintFormHeader),
                notification: () => this.createPageObject(Notification),
                controls: () => this.createPageObject(Controls),
            });
            await this.commentSnippet.isMoreActionsExist()
                .should.eventually.to.be.equal(true, 'Троеточие отображается на сниппете.');
            await this.browser.yaWaitForChangeValue({
                action: () => this.commentSnippet.moreActionsClick(),
                valueGetter: () => this.controls.isVisible(),
            });
            await this.controls.clickComplainButton();
            await this.complaintForm.waitForContentVisible();
        },
        'Форма жалобы': {
            'при нажатии кнопки «Пожаловаться»': {
                открывается: makeCase({
                    id: 'm-touch-2341',
                    issue: 'MOBMARKET-10694',
                    feature: 'Жалоба на комментарий',
                    async test() {
                        await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(true, 'Кнопка «Отправить» не активна.');
                    },
                }),
            },
            'при выбранном типе жалобы, отличном от «Другая»': {
                'имеет активную кнопку «Отправить»': makeCase({
                    id: 'm-touch-2342',
                    issue: 'MOBMARKET-10695',
                    feature: 'Жалоба на комментарий',
                    async test() {
                        await this.complaintForm.clickReasonByIndex(1);
                        await this.complaintForm.waitForCheckedReasonByIndex(1)
                            .should.eventually.to.be.equal(true, 'Выбран пункт «Спам и реклама»');
                        await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка «Отправить» активна.');
                        await this.complaintForm.clickReasonByIndex(2);
                        await this.complaintForm.waitForCheckedReasonByIndex(2)
                            .should.eventually.to.be.equal(true, 'Выбран пункт «Оскорбительный контент»');
                        await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка «Отправить» активна.');
                    },
                }),
                'не показывает текстовое поле ввода жалобы': makeCase({
                    id: 'm-touch-2356',
                    issue: 'MOBMARKET-10693',
                    feature: 'Жалоба на комментарий',
                    async test() {
                        await this.complaintForm.waitForInputHidden();
                        await this.complaintForm.clickReasonByIndex(1);
                        await this.complaintForm.waitForCheckedReasonByIndex(1)
                            .should.eventually.to.be.equal(true, 'Выбран пункт «Спам и реклама»');
                        await this.complaintForm.waitForInputHidden();
                        await this.complaintForm.clickReasonByIndex(2);
                        await this.complaintForm.waitForCheckedReasonByIndex(2)
                            .should.eventually.to.be.equal(true, 'Выбран пункт «Оскорбительный контент»');
                        await this.complaintForm.waitForInputHidden();
                    },
                }),
                'при нажатии на кнопку «Отправить»': {
                    'успешно отправляется': makeCase({
                        id: 'm-touch-2516',
                        issue: 'MOBMARKET-10810',
                        feature: 'Жалоба на комментарий',
                        async test() {
                            await this.complaintForm.clickReasonByIndex(1);
                            await this.complaintForm.waitForCheckedReasonByIndex(1)
                                .should.eventually.to.be.equal(true, 'Выбрана первая жалоба');
                            await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(false, 'Кнопка «Отправить» активна.');
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintForm.waitForContentHidden();
                            await this.notification
                                .getText()
                                .should.eventually.be.equal(
                                    'Спасибо! Мы проверим комментарий и удалим его при необходимости'
                                );
                        },
                    }),
                },
            },
            'при выбранном типе жалобы «Другая»': {
                'отображается поле ввода текста жалобы': makeCase({
                    id: 'm-touch-2343',
                    issue: 'MOBMARKET-10696',
                    feature: 'Жалоба на комментарий',
                    async test() {
                        await this.complaintForm.clickReasonByIndex(3);
                        await this.complaintForm.waitForCheckedReasonByIndex(3)
                            .should.eventually.to.be.equal(true, 'Выбран пункт «Другое»');
                        // переходим на страницу ввода текста
                        await this.complaintFormSubmitButton.clickSubmitButton();
                        await this.complaintForm.waitForInputVisible();
                        await this.complaintFormHeader.clickPreviousIcon();
                        // Если выбрать тип жалобы, отличную от Другая, после выбора жалобы Другая, то поле скроется
                        await this.complaintForm.clickReasonByIndex(1);
                        await this.complaintForm.waitForCheckedReasonByIndex(1)
                            .should.eventually.to.be.equal(true, 'Выбран пункт «Спам и реклама»');
                        await this.complaintForm.waitForInputHidden();
                    },
                }),
                'без введенного текста жалобы': {
                    'не имеет активную кнопку «Отправить»': makeCase({
                        id: 'm-touch-2352',
                        issue: 'MOBMARKET-10687',
                        feature: 'Жалоба на комментарий',
                        async test() {
                            await this.complaintForm.clickReasonByIndex(3);
                            await this.complaintForm.waitForCheckedReasonByIndex(3)
                                .should.eventually.to.be.equal(true, 'Выбрана жалоба «Другая»');
                            // переходим на страницу ввода текста
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(true, 'Кнопка «Отправить» не активна.');
                        },
                    }),
                },
                'при наличии введенного текста жалобы': {
                    'имеет активную кнопку «Отправить»': makeCase({
                        id: 'm-touch-2353',
                        issue: 'MOBMARKET-10688',
                        feature: 'Жалоба на комментарий',
                        async test() {
                            await this.complaintForm.clickReasonByIndex(3);
                            await this.complaintForm.waitForCheckedReasonByIndex(3)
                                .should.eventually.to.be.equal(true, 'Выбрана жалоба «Другая»');
                            // переходим на страницу ввода текста
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintForm.waitForInputVisible();
                            await this.complaintForm.setTextFieldInput('Ябеда');
                            await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(false, 'Кнопка «Отправить» активна.');
                        },
                    }),
                    'при нажатии кнопки «Отправить»': {
                        'успешно отправляется': makeCase({
                            id: 'm-touch-2354',
                            issue: 'MOBMARKET-10689',
                            feature: 'Жалоба на комментарий',
                            async test() {
                                await this.complaintForm.clickReasonByIndex(3);
                                await this.complaintForm.waitForCheckedReasonByIndex(3)
                                    .should.eventually.to.be.equal(true, 'Выбрана жалоба «Другая»');
                                // переходим на страницу ввода текста
                                await this.complaintFormSubmitButton.clickSubmitButton();
                                await this.complaintForm.waitForInputVisible();
                                await this.complaintForm.setTextFieldInput('Ябеда');
                                await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка «Отправить» активна.');
                                await this.complaintFormSubmitButton.clickSubmitButton();
                                await this.complaintForm.waitForContentHidden();
                                await this.notification
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Спасибо! Мы проверим комментарий и удалим его при необходимости'
                                    );
                            },
                        }),
                    },
                },
            },
            'при нажатии на иконку «Стрелка» в заголовке': {
                закрывается: makeCase({
                    id: 'm-touch-2339',
                    issue: 'MOBMARKET-10690',
                    feature: 'Жалоба на комментарий',
                    async test() {
                        await this.complaintFormHeader.clickCloseIcon();
                        await this.complaintForm.waitForContentHidden();
                    },
                }),
            },
            'при выбранной жалобе «Другая», введенном тексте жалобы и нажатии кнопки «Отмена»': {
                'закрывается, жалоба пропадает': makeCase({
                    id: 'm-touch-2355',
                    issue: 'MOBMARKET-10691',
                    feature: 'Жалоба на комментарий',
                    async test() {
                        await this.complaintForm.clickReasonByIndex(3);
                        await this.complaintForm.waitForCheckedReasonByIndex(3)
                            .should.eventually.to.be.equal(true, 'Выбрана жалоба «Другая»');
                        // переходим на страницу ввода текста
                        await this.complaintFormSubmitButton.clickSubmitButton();
                        await this.complaintForm.waitForInputVisible();
                        await this.complaintForm.setTextFieldInput('Ябеда');
                        await this.complaintForm.getInputText()
                            .should.eventually.to.be.equal('Ябеда', 'Введенный текст соответствует заданному.');
                        await this.complaintFormHeader.clickCloseIcon();
                        await this.complaintForm.waitForContentHidden();
                        // повторно открываем окно с жалобами
                        await this.commentSnippet.moreActionsClick();
                        await this.controls.isVisible();
                        await this.controls.clickComplainButton();
                        await this.complaintForm.waitForContentVisible();
                        await this.complaintForm.isCheckedReasonByIndex(3)
                            .should.eventually.to.be.equal(false, 'Не выбрана жалоба «Другая».');
                        await this.complaintForm.clickReasonByIndex(3);
                        await this.complaintForm.waitForCheckedReasonByIndex(3)
                            .should.eventually.to.be.equal(true, 'Выбрана жалоба «Другая».');
                        // переходим на страницу ввода текста
                        await this.complaintFormSubmitButton.clickSubmitButton();
                        await this.complaintForm.waitForInputVisible();
                        await this.complaintForm.getInputText()
                            .should.eventually.to.be.equal('', 'Введенный текст отсутствует.');
                    },
                }),
            },
        },
    },
});
