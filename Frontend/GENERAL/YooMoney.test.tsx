import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnYooMoney,
    YooMoneyDesktop,
    YooMoneyTouchPhone,
} from '.';

describe('YooMoney', () => {
    test('cnYooMoney', () => {
        expect(cnYooMoney()).toBe('YpcYooMoney');
    });

    for (const [platform, YooMoney] of Object.entries({
        desktop: YooMoneyDesktop,
        'touch-phone': YooMoneyTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <YooMoney
                            className="Test"
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
                                <YooMoney
                                    view={view}
                                />,
                            )
                            .toJSON();

                        expect(tree).toMatchSnapshot();
                    });
                }
            });
        });
    }
});
