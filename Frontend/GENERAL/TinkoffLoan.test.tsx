import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnTinkoffLoan,
    TinkoffLoanDesktop,
    TinkoffLoanTouchPhone,
} from '.';

describe('TinkoffLoan', () => {
    test('cnTinkoffLoan', () => {
        expect(cnTinkoffLoan()).toBe('YpcTinkoffLoan');
    });

    for (const [platform, TinkoffLoan] of Object.entries({
        desktop: TinkoffLoanDesktop,
        'touch-phone': TinkoffLoanTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <TinkoffLoan
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
                                <TinkoffLoan
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
