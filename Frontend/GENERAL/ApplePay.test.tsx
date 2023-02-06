import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnApplePay,
    ApplePayDesktop,
    ApplePayTouchPhone,
} from '.';

describe('ApplePay', () => {
    test('cnApplePay', () => {
        expect(cnApplePay()).toBe('YpcApplePay');
    });

    for (const [platform, ApplePay] of Object.entries({
        desktop: ApplePayDesktop,
        'touch-phone': ApplePayTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <ApplePay
                            className="Test"
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
                                <ApplePay
                                    view={view}
                                />
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });
        });
    }
});
