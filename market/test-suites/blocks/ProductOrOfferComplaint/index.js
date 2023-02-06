import {makeCase, makeSuite} from 'ginny';
import {THX_PRODUCT_OFFER_MESSAGE} from '@self/platform/entities/complaintForm/constants';

/**
 * @param {PageObject.ComplaintForm} complaintForm
 * @param {PageObject.ComplaintFormSubmitButton} complaintFormSubmitButton
 * @param {PageObject.Notification} notification
 */
const DEFAULT_OTHER_INDEX = 8;

export default makeSuite('Жалоба на товар или оффер', {
    issue: 'MARKETFRONT-9669',
    feature: 'Жалоба на товар/оффер',
    story: {
        'По умолчанию': {
            'кнопка "пожаловаться" не активна': makeCase({
                id: 'm-touch-3265',
                async test() {
                    await this.complaintForm.waitForContentVisible();
                    await this.complaintFormSubmitButton.waitForVisible();
                    await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                        .should
                        .eventually
                        .to
                        .be
                        .equal(true, 'Кнопка «пожаловаться» не активна.');
                },
            }),
        },

        'При выборе "другое"': {
            'кнопка "пожаловаться" не активна': makeCase({
                id: 'm-touch-3265',
                async test() {
                    await this.complaintForm.waitForContentVisible();
                    await this.complaintForm.clickReasonByIndex(this.params.otherIndex || DEFAULT_OTHER_INDEX);
                    // переходим на страницу ввода текста
                    await this.complaintFormSubmitButton.clickSubmitButton();
                    await this.complaintFormSubmitButton.isSubmitButtonDisabled()
                        .should
                        .eventually
                        .to
                        .be
                        .equal(true, 'Кнопка «пожаловаться» не активна.');
                },
            }),
        },

        'При нажатии на крестик': {
            'форма должна закрыться': makeCase({
                id: 'm-touch-3263',
                async test() {
                    await this.complaintForm.waitForContentVisible();
                    await this.complaintFormHeader.clickCloseIcon();
                    await this.complaintForm.waitForContentHidden();
                },
            }),
        },

        'При успешной отправке': {
            'При выборе причины #1 (для оффера - сигнал на апи ABO без создания тикета)': {
                'Без заполнения текстового поля и по клику "пожаловаться"': {
                    'Должны увидеть сообщение об успешной отправке': makeCase({
                        id: 'm-touch-3264',
                        async test() {
                            await this.complaintForm.waitForContentVisible();
                            await this.complaintForm.clickReasonByIndex(1);
                            // переходим на страницу ввода текста
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintForm.waitForContentHidden();
                            await this.notification
                                .getText()
                                .should
                                .eventually
                                .be
                                .equal(THX_PRODUCT_OFFER_MESSAGE);
                        },
                    }),
                },
            },
            'При выборе причины "Другое"': {
                'C заполнением текстового поля и по клику "пожаловаться"': {
                    'Должны увидеть сообщение об успешной отправке': makeCase({
                        id: 'm-touch-3264',
                        async test() {
                            await this.complaintForm.waitForContentVisible();
                            await this.complaintForm.clickReasonByIndex(this.params.otherIndex || DEFAULT_OTHER_INDEX);
                            // переходим на страницу ввода текста
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintForm.setTextFieldInput('Ябеда');
                            await this.complaintForm.setTextFieldNameInput('Ябеда Корябеда');
                            await this.complaintForm.setTextFieldEmailInput('yabeda@qwe.qwe');
                            await this.complaintFormSubmitButton.clickSubmitButton();
                            await this.complaintForm.waitForContentHidden();
                            await this.notification
                                .getText()
                                .should
                                .eventually
                                .be
                                .equal(THX_PRODUCT_OFFER_MESSAGE);
                        },
                    }),
                },
            },
        },
    },
});
