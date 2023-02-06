import React from 'react';
import renderer from 'react-test-renderer';

import {
    View,
} from '../core';
import {
    cnFasterPaymentsSystem,
    FasterPaymentsSystemDesktop,
    FasterPaymentsSystemTouchPhone,
} from '.';

describe('FasterPaymentsSystem', () => {
    test('cnFasterPaymentsSystem', () => {
        expect(cnFasterPaymentsSystem()).toBe('YpcFasterPaymentsSystem');
    });

    for (const [platform, FasterPaymentsSystem] of Object.entries({
        desktop: FasterPaymentsSystemDesktop,
        'touch-phone': FasterPaymentsSystemTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <FasterPaymentsSystem
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
                                <FasterPaymentsSystem
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
