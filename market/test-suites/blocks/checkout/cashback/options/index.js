import {makeSuite, mergeSuites} from 'ginny';

// pageObject
import CheckoutSummary
    from '@self/root/src/components/CheckoutSummary/__pageObject';
import CheckoutCashbackontrol
    from '@self/root/src/components/CheckoutCashback/__pageObject';
import {CashbackOptionSelect} from
    '@self/root/src/components/CheckoutCashback/components/CashbackOptionSelect/__pageObject';

import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import {CashbackSpendTotal} from '@self/root/src/components/OrderTotal/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';

// constants
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {yandexPlusPerk, yandexCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import {CASHBACK_PROFILE_TYPES} from '@self/root/src/entities/cashbackProfile';

// suite maker :)
import {makeDefaultCasesForCheckoutCashbackOptions} from './suites';

// prepare kadavr state
import {prepareCheckoutPageWithCashback, prepareActualizedCheckoutResponse} from '../utils';

const CASHBACK_AMOUNT = 123;

export default makeSuite('Опции выбора кешбэка.', {
    feature: 'Кешбэк',
    issue: 'MARKETFRONT-24080',
    environment: 'kadavr',
    params: {
        cashbackAmount: 'Количество кешбэка списываемого/начисляемого за заказ',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
        perks: [yandexPlusPerk, yandexCashbackPerk],
        cashbackAmount: CASHBACK_AMOUNT,
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                cashbackControl: () => this.createPageObject(CheckoutCashbackontrol, {
                    parent: this.checkoutPage,
                }),
                cashbackOptionSelect: () => this.createPageObject(CashbackOptionSelect, {
                    parent: this.cashbackControl,
                }),

                checkoutSummary: () => this.createPageObject(CheckoutSummary, {
                    parent: this.checkoutPage,
                }),
                cashbackSummaryInfo: () => this.createPageObject(CashbackInfo, {
                    parent: this.checkoutSummary,
                }),
                cashbackSpendTotal: () => this.createPageObject(CashbackSpendTotal, {
                    parent: this.checkoutSummary,
                }),
                paymentOptionsModal: () => this.createPageObject(Modal),
                paymentOptionsList: () => this.createPageObject(PaymentOptionsList),
            });
        },
        'Заказ с возможностью начисления, но без списания.': {
            beforeEach() {
                return this.browser.yaScenario(
                    this,
                    prepareCheckoutPageWithCashback,
                    {
                        allowEmit: true,
                        allowSpend: false,
                        defaultSelectedOption: CASHBACK_PROFILE_TYPES.EMIT,
                    }
                );
            },
            'По умолчанию': makeDefaultCasesForCheckoutCashbackOptions({
                id: 'marketfront-4106',
                isAllowEmit: true,
                isAllowSpend: false,
                cashbackAmount: CASHBACK_AMOUNT,
            }),
            'При изменении способов оплаты': {
                'получение баллов остается доступным.': {
                    // Выбираем способ оплаты наличными при получении
                    async beforeEach() {
                        await this.editableCard.changeButtonClick();
                        await this.paymentOptionsModal.waitForVisible();
                        await this.paymentOptionsList.setPaymentTypeCashOnDelivery();
                        await this.paymentOptionsList.submitButtonClick();
                    },
                    ...makeDefaultCasesForCheckoutCashbackOptions({
                        id: 'marketfront-4106',
                        isAllowEmit: true,
                        isAllowSpend: false,
                        cashbackAmount: CASHBACK_AMOUNT,
                    }),
                },
            },
        },
        'Заказ с возможностью списания и начисления.': {
            beforeEach() {
                return this.browser.yaScenario(
                    this,
                    prepareCheckoutPageWithCashback,
                    {
                        allowEmit: true,
                        allowSpend: true,
                        defaultSelectedOption: CASHBACK_PROFILE_TYPES.SPEND,
                    }
                );
            },
            'По умолчанию': makeDefaultCasesForCheckoutCashbackOptions({
                id: 'marketfront-4107',
                isAllowEmit: true,
                isAllowSpend: true,
                cashbackAmount: CASHBACK_AMOUNT,
            }),
            'При изменении способа оплаты': {
                'выбор переключается на получения баллов.': {
                    async beforeEach() {
                        await this.browser.yaScenario(
                            this,
                            prepareActualizedCheckoutResponse,
                            {
                                isEmitAllow: true,
                                isSpendAllow: false,
                                cashbackAmount: CASHBACK_AMOUNT,
                            }
                        );
                        await this.editableCard.changeButtonClick();
                        await this.paymentOptionsList.setPaymentTypeCashOnDelivery();
                        await this.paymentOptionsList.submitButtonClick();
                    },
                    ...makeDefaultCasesForCheckoutCashbackOptions({
                        id: 'marketfront-4107',
                        isAllowEmit: true,
                        isAllowSpend: false,
                        cashbackAmount: CASHBACK_AMOUNT,
                    }),
                },
            },
        },
    }),
});
