import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnFooter,
    FooterDesktop,
    FooterTouchPhone,
} from '.';

describe('Footer', () => {
    test('cnFooter', () => {
        expect(cnFooter()).toBe('YpcFooter');
    });

    for (const [platform, Footer] of Object.entries({
        desktop: FooterDesktop,
        'touch-phone': FooterTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <Footer
                            className="Test"
                        />,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('children', () => {
                const tree = renderer
                    .create(
                        <Footer>
                            <div>
                                Children
                            </div>
                        </Footer>,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('childrenAfter', () => {
                const tree = renderer
                    .create(
                        <Footer
                            childrenAfter={<div>ChildrenAfter</div>}
                        >
                            <div>
                                Children
                            </div>
                        </Footer>,
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
