import React from 'react';
import renderer from 'react-test-renderer';

import {
    BankCardIssuer,
    BankCardPaySystem,
    FamilyCardLimitType,
    Currency,
    View,
} from '../core';
import {
    cnFamilyCard,
    FamilyCardDesktop,
    FamilyCardTouchPhone,
} from '.';

describe('FamilyCard', () => {
    test('cnFamilyCard', () => {
        expect(cnFamilyCard()).toBe('YpcFamilyCard');
    });

    for (const [platform, FamilyCard] of Object.entries({
        desktop: FamilyCardDesktop,
        'touch-phone': FamilyCardTouchPhone,
    })) {
        describe(platform, () => {
            test('number', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            number="1234 5678 9876 5432"
                            view={View.Vertical}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('balance', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            view={View.Vertical}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('currency', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            currency={Currency.USD}
                            view={View.Vertical}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('limitType', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            limitType={FamilyCardLimitType.Month}
                            view={View.Vertical}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            describe('view', () => {
                for (const view of [View.Vertical, View.Horizontal]) {
                    test(view, () => {
                        const tree = renderer
                            .create(
                                <FamilyCard
                                    balance={100500}
                                    view={view}
                                />,
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });

            test('issuer', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            issuer={BankCardIssuer.TinkoffBank}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('paySystem', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            paySystem={BankCardPaySystem.Mastercard}
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <FamilyCard
                            balance={100500}
                            className="Test"
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
