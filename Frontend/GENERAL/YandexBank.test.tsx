import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnYandexBank,
    YandexBankDesktop,
    YandexBankTouchPhone,
} from '.';

describe('YandexBank', () => {
    test('cnYandexBank', () => {
        expect(cnYandexBank()).toBe('YpcYandexBank');
    });

    for (const [platform, YandexBank] of Object.entries({
        desktop: YandexBankDesktop,
        'touch-phone': YandexBankTouchPhone,
    })) {
        describe(platform, () => {
            describe('view', () => {
                for (const view of [View.Vertical, View.Horizontal]) {
                    test(view, () => {
                        const tree = renderer
                            .create(
                                <YandexBank
                                    view={view}
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
                        <YandexBank
                            className="Test"
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
