import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnPaymentMethod,
    PaymentMethodDesktop,
    PaymentMethodTouchPhone,
} from '.';

describe('PaymentMethod', () => {
    test('cnPaymentMethod', () => {
        expect(cnPaymentMethod()).toBe('YpcPaymentMethod');
    });

    for (const [platform, PaymentMethod] of Object.entries({
        desktop: PaymentMethodDesktop,
        'touch-phone': PaymentMethodTouchPhone,
    })) {
        describe(platform, () => {
            test('childrenBefore', () => {
                const tree = renderer
                    .create(
                        <PaymentMethod
                            name="PaymentMethod"
                            view={View.Vertical}
                            childrenBefore={(
                                <i>ChildrenBefore</i>
                            )}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('children', () => {
                const tree = renderer
                    .create(
                        <PaymentMethod
                            name="PaymentMethod"
                            view={View.Horizontal}
                        >
                            Children
                        </PaymentMethod>
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('childrenAfter', () => {
                const tree = renderer
                    .create(
                        <PaymentMethod
                            name="PaymentMethod"
                            view={View.Vertical}
                            childrenAfter={(
                                <i>СhildrenAfter</i>
                            )}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <PaymentMethod
                            name="MasterCard"
                            view={View.Horizontal}
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
