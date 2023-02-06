import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnNewBankCard,
    NewBankCardDesktop,
    NewBankCardTouchPhone,
} from '.';

describe('NewBankCard', () => {
    test('cnNewBankCard', () => {
        expect(cnNewBankCard()).toBe('YpcNewBankCard');
    });

    for (const [platform, NewBankCard] of Object.entries({
        desktop: NewBankCardDesktop,
        'touch-phone': NewBankCardTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <NewBankCard
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
                                <NewBankCard
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
