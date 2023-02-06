import React from 'react';
import renderer from 'react-test-renderer';

import {
    MethodType,
} from '../core';
import {
    cnPaymentButton,
    PaymentButtonDesktop,
    PaymentButtonTouchPhone,
} from '.';

describe('PaymentButton', () => {
    test('cnPaymentButton', () => {
        expect(cnPaymentButton()).toBe('YpcPaymentButton');
    });

    for (const [platform, PaymentButton] of Object.entries({
        desktop: PaymentButtonDesktop,
        'touch-phone': PaymentButtonTouchPhone,
    })) {
        describe(platform, () => {
            describe('methodType', () => {
                test(MethodType.BankCard, () => {
                    const tree = renderer
                        .create(
                            <PaymentButton
                                methodType={MethodType.BankCard}
                                children="Оплата"
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });

                test(MethodType.NewBankCard, () => {
                    const tree = renderer
                        .create(
                            <PaymentButton
                                methodType={MethodType.NewBankCard}
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });

                test(MethodType.ApplePay, () => {
                    const tree = renderer
                        .create(
                            <PaymentButton
                                methodType={MethodType.ApplePay}
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });

                test(MethodType.GooglePay, () => {
                    const tree = renderer
                        .create(
                            <PaymentButton
                                methodType={MethodType.GooglePay}
                            />,
                        )
                        .toJSON();

                    expect(tree).toMatchSnapshot();
                });

                test(MethodType.FasterPaymentsSystem, () => {
                    const tree = renderer
                        .create(
                            <PaymentButton
                                methodType={MethodType.FasterPaymentsSystem}
                            />,
                        )
                        .toJSON();
                    expect(tree).toMatchSnapshot();
                });
            });

            describe('isYLogo', () => {
                for (const methodType of [MethodType.BankCard, MethodType.NewBankCard, MethodType.FamilyCard]) {
                    test(methodType, () => {
                        const tree = renderer
                            .create(
                                <PaymentButton
                                    methodType={methodType}
                                    isYLogo
                                />,
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });

            describe('totalPrice', () => {
                for (const methodType of [MethodType.BankCard, MethodType.NewBankCard, MethodType.FamilyCard]) {
                    test(methodType, () => {
                        const tree = renderer
                            .create(
                                <PaymentButton
                                    methodType={methodType}
                                    totalPrice="1 000 ₽"
                                />,
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <PaymentButton
                            className="Test"
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
