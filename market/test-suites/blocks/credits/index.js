// блок с кредитом
import {makeSuite, makeCase} from 'ginny';

import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';

import CreditInfo from '@self/root/src/components/CreditInfo/__pageObject';
import EditPaymentOption from
    '@self/root/src/components/EditPaymentOption/__pageObject';
import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';

import {
    skuMock as skuKettle,
    expensiveOfferMock as offerKettleCredit,
    offerMock as offerKettle,
} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

export const creditWidgetSuite = makeSuite('Кредит доступен.', {
    feature: 'Кредиты',
    issue: 'MARKETFRONT-37112',
    environment: 'kadavr',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        item: {
            sku: skuKettle,
            offer: offerKettleCredit,
            count: 1,
        },
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                creditInfo: () => this.createPageObject(CreditInfo),
                confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                paymentOptions: () => this.createPageObject(EditPaymentOption),
            });
        },
        'Отображается кредитное предложение.': makeCase({
            async test() {
                const isExisting = await this.creditInfo.isExisting();

                await this.expect(isExisting).be.equal(
                    true,
                    'Отображается блок с кредитом'
                );
            },
        }),
        'Переходим по "Оформить".': makeCase({
            async test() {
                // ставим последний выбранный способ оплаты постоплату чтобы убедится что
                // встанет кредит в способ оплаты
                await this.browser.setState('persAddress.lastState', {
                    paymentType: PAYMENT_TYPE.POSTPAID,
                    paymentMethod: PAYMENT_METHOD.CARD_ON_DELIVERY,
                    contactId: null,
                    parcelsInfo: null,
                });

                await this.creditInfo.button.click();
                await this.confirmationPage.waitForVisible(5000);
                await this.browser.yaDelay(3000); // ожидаем завершение клиентской актуализации

                const paymentText = await this.paymentOptions.getText();
                await this.expect(paymentText).be.include(
                    'В кредит от Тинькофф',
                    'На карточке способа оплаты должен отображатся способ оплаты "В кредит от Тинькофф"'
                );
            },
        }),
    },
});

export const creditWidgetWithoutSuite = makeSuite('Кредит не доступен.', {
    feature: 'Кредиты',
    environment: 'kadavr',
    issue: 'MARKETFRONT-37112',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        item: {
            sku: skuKettle,
            offer: offerKettle,
            count: 1,
        },
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                creditInfo: () => this.createPageObject(CreditInfo),
            });
        },
        'Нет блока с кредитом.': makeCase({
            async test() {
                const isExisting = await this.creditInfo.isExisting();

                await this.expect(isExisting).be.equal(
                    false,
                    'Не отображается блок с кредитом'
                );
            },
        }),
    },
});
