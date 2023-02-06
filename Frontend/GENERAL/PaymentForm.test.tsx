import React from 'react';
import renderer from 'react-test-renderer';

import {
    MethodType,
    BankCardIssuer,
    BankCardPaySystem,
} from '../core';
import {
    cnPaymentForm,
    PaymentFormDesktop,
    PaymentFormTouchPhone,
} from '.';

describe('PaymentForm', () => {
    test('cnPaymentForm', () => {
        expect(cnPaymentForm()).toBe('YpcPaymentForm');
    });

    for (const [platform, PaymentForm] of Object.entries({
        desktop: PaymentFormDesktop,
        'touch-phone': PaymentFormTouchPhone,
    })) {
        describe(platform, () => {
            test('default', () => {
                const tree = renderer
                    .create(
                        <PaymentForm
                            totalPrice="1 000 ₽"
                            originalPrice="1 200 ₽"
                            childrenTitle="ООО «Кусочек лета»"
                            childrenSubtitle="Заказ №9031"
                            childrenFooter="Платеж получит ООО «Brebs»"
                            timeStamp={1200}
                            dieHardUrl="https://diehard.yandex.ru/web/card_form"
                            purchaseToken="yandex-pay"
                            methods={[{
                                methodId: 'card-x3230',
                                methodType: MethodType.BankCard,
                                number: '400000****3604',
                                issuer: BankCardIssuer.TinkoffBank,
                                paySystem: BankCardPaySystem.Visa,
                            }, {
                                methodId: 'card-x7603',
                                methodType: MethodType.BankCard,
                                number: '500000****2986',
                                issuer: BankCardIssuer.SberBank,
                                paySystem: BankCardPaySystem.MIR,
                            }, {
                                methodId: 'card-x3231',
                                methodType: MethodType.BankCard,
                                number: '860002****2222',
                                paySystem: BankCardPaySystem.Uzcard,
                            }, {
                                methodId: 'card-x3232',
                                methodType: MethodType.BankCard,
                                number: '411111****1113',
                                issuer: BankCardIssuer.AlfaBank,
                                paySystem: BankCardPaySystem.Visa,
                            }]}
                            isEmailInput
                            isPaymentButtonDisabled
                            BeforePaymentButton={({ page }) => (
                                <div>BeforePaymentButton: {page}</div>
                            )}
                            BeforeFooter={({ page }) => (
                                <div>BeforeFooter: {page}</div>
                            )}
                        >
                            children
                        </PaymentForm>
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
