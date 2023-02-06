import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnGooglePay,
    GooglePayDesktop,
    GooglePayTouchPhone,
} from '.';

describe('GooglePay', () => {
    test('cnGooglePay', () => {
        expect(cnGooglePay()).toBe('YpcGooglePay');
    });

    for (const [platform, GooglePay] of Object.entries({
        desktop: GooglePayDesktop,
        'touch-phone': GooglePayTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <GooglePay
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
                                <GooglePay
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
