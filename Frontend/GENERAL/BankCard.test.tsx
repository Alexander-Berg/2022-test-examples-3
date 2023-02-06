import React from 'react';
import renderer from 'react-test-renderer';

import {
    BankCardIssuer,
    BankCardPaySystem,
    View,
    Size,
} from '../core';
import {
    cnBankCard,
    BankCardDesktop,
    BankCardTouchPhone,
} from '.';

describe('BankCard', () => {
    test('cnBankCard', () => {
        expect(cnBankCard()).toBe('YpcBankCard');
    });

    for (const [platform, BankCard] of Object.entries({
        desktop: BankCardDesktop,
        'touch-phone': BankCardTouchPhone,
    })) {
        describe(platform, () => {
            test('number', () => {
                const tree = renderer
                    .create(
                        <BankCard
                            number="1234 5678 9876 5432"
                            view={View.Vertical}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            describe('view', () => {
                for (const view of [View.Vertical, View.Horizontal]) {
                    test(view, () => {
                        const tree = renderer
                            .create(
                                <BankCard
                                    number="1234"
                                    view={view}
                                />
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });

            describe('size', () => {
                for (const size of [Size.Compact, Size.Bulky]) {
                    test(size, () => {
                        const tree = renderer
                            .create(
                                <BankCard
                                    number="1234"
                                    size={size}
                                />
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });

            test('issuer', () => {
                const tree = renderer
                    .create(
                        <BankCard
                            number="5678 9876"
                            issuer={BankCardIssuer.TinkoffBank}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('paySystem', () => {
                const tree = renderer
                    .create(
                        <BankCard
                            number="1234"
                            paySystem={BankCardPaySystem.Mastercard}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <BankCard
                            number="4390"
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
