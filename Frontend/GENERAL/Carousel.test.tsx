import React from 'react';
import renderer from 'react-test-renderer';

import {
    MethodType,
    Currency,
    BankCardIssuer,
    BankCardPaySystem,
    FamilyCardLimitType,
} from '../core';
import {
    cnCarousel,
    CarouselDesktop,
    CarouselTouchPhone,
} from '.';

describe('Carousel', () => {
    test('cnCarousel', () => {
        expect(cnCarousel()).toBe('YpcCarousel');
    });

    for (const [platform, Carousel] of Object.entries({
        desktop: CarouselDesktop,
        'touch-phone': CarouselTouchPhone,
    })) {
        describe(platform, () => {
            test('methodId & methods', () => {
                const tree = renderer
                    .create(
                        <Carousel
                            methodId="card-x5643"
                            methods={[{
                                methodId: 'card-x5643',
                                methodType: MethodType.BankCard,
                                number: '5643',
                                issuer: BankCardIssuer.TinkoffBank,
                                paySystem: BankCardPaySystem.Mastercard,
                            }, {
                                methodId: 'custom-method-1',
                                methodType: MethodType.Custom,
                                name: 'Кастомный метод 1',
                            }, {
                                methodId: 'card-x2007',
                                methodType: MethodType.BankCard,
                                number: '2007',
                                issuer: BankCardIssuer.SberBank,
                                paySystem: BankCardPaySystem.Visa,
                            }, {
                                methodId: 'card-x2411',
                                methodType: MethodType.BankCard,
                                methodDisabled: true,
                                number: '2411',
                                issuer: BankCardIssuer.VTBBank,
                                paySystem: BankCardPaySystem.MIR,
                            }, {
                                methodId: 'custom-method-2',
                                methodType: MethodType.Custom,
                                name: 'Кастомный метод 2',
                            }, {
                                methodId: 'card-x1234',
                                methodType: MethodType.FamilyCard,
                                issuer: BankCardIssuer.SberBank,
                                paySystem: BankCardPaySystem.Mastercard,
                                balance: 1500,
                                currency: Currency.RUB,
                                limitType: FamilyCardLimitType.Month,
                            }]}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <Carousel
                            methodId="card-x5643"
                            methods={[{
                                methodId: 'card-x5643',
                                methodType: MethodType.BankCard,
                                number: '5643',
                                issuer: BankCardIssuer.TinkoffBank,
                                paySystem: BankCardPaySystem.Mastercard,
                            }, {
                                methodId: 'card-x2007',
                                methodType: MethodType.BankCard,
                                number: '2007',
                                issuer: BankCardIssuer.SberBank,
                                paySystem: BankCardPaySystem.Visa,
                            }]}
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
