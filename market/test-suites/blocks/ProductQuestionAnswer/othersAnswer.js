import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ComplaintForm} complaintForm
 * @param {PageObject.ContentManageControls} contentManageControls
 * @param {PageObject.Comment.Controls} controls
 * @param {PageObject.ComplaintFormSubmitButton} complaintFormSubmitButton
 * @param {PageObject.ComplaintFormHeader} complaintFormHeader
 * @param {PageObject.Notification} notification
 */
export default makeSuite('Сниппет чужого ответа.', {
    environment: 'kadavr',
    issue: 'MOBMARKET-9507',
    feature: 'Жалоба на ответ',
    story: {
        async beforeEach() {
            await this.contentManageControls.isControlsButtonVisible();
            await this.contentManageControls.clickControlsButton();
            await this.controls.clickComplainButton();
            await this.complaintForm.waitForContentVisible();
        },
        'Форма жалобы на ответ': {
            'при нажатии кнопки «Пожаловаться»': {
                'открывается': makeCase({
                    id: 'm-touch-2341',
                    async test() {
                        await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(true, 'Кнопка «Отправить» не активна.');
                    },
                }),
            },
            'при выбранном типе жалобы, отличном от «Другая»': {
                'имеет активную кнопку «Отправить»': makeCase({
                    id: 'm-touch-2342',
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
                                    'Спасибо! Мы проверим ответ и удалим его при необходимости');
                        },
                    }),
                },
            },
            'при выбранном типе жалобы «Другая»': {
                'отображается поле ввода текста жалобы': makeCase({
                    id: 'm-touch-2343',
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
                                        'Спасибо! Мы проверим ответ и удалим его при необходимости');
                            },
                        }),
                    },
                },
            },
            'при нажатии на иконку «Крестик» в заголовке': {
                'закрывается': makeCase({
                    id: 'm-touch-2339',
                    async test() {
                        await this.complaintFormHeader.clickCloseIcon();
                        await this.complaintForm.waitForContentHidden();
                    },
                }),
            },
            'при выбранной жалобе «Другая», введенном тексте жалобы и нажатии кнопки «Отмена»': {
                'закрывается, жалоба пропадает': makeCase({
                    id: 'm-touch-2355',
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
                        await this.contentManageControls.isControlsButtonVisible();
                        await this.contentManageControls.clickControlsButton();
                        await this.controls.clickComplainButton();
                        await this.complaintForm.waitForContentVisible();
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
