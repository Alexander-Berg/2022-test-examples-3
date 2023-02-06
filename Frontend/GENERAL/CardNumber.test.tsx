import React from 'react';
import renderer from 'react-test-renderer';

import {
    Size,
} from '../core';
import {
    cnCardNumber,
    CardNumberDesktop,
    CardNumberTouchPhone,
} from '.';

describe('CardNumber', () => {
    test('cnCardNumber', () => {
        expect(cnCardNumber()).toBe('YpcCardNumber');
    });

    for (const [platform, CardNumber] of Object.entries({
        desktop: CardNumberDesktop,
        'touch-phone': CardNumberTouchPhone,
    })) {
        describe(platform, () => {
            test('number', () => {
                const tree = renderer
                    .create(
                        <CardNumber
                            number="1234 5678 9876 5432"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            describe('size', () => {
                for (const size of [Size.Compact, Size.Bulky]) {
                    test(size, () => {
                        const tree = renderer
                            .create(
                                <CardNumber
                                    number="1234"
                                    size={size}
                                />
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <CardNumber
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
