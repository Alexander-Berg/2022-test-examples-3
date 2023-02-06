import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnHeader,
    HeaderDesktop,
    HeaderTouchPhone,
} from '.';

describe('Header', () => {
    test('cnHeader', () => {
        expect(cnHeader()).toBe('YpcHeader');
    });

    for (const [platform, Header] of Object.entries({
        desktop: HeaderDesktop,
        'touch-phone': HeaderTouchPhone,
    })) {
        describe(platform, () => {
            test('totalPrice', () => {
                const tree = renderer
                    .create(
                        <Header
                            totalPrice="432 ₽"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('originalPrice', () => {
                const tree = renderer
                    .create(
                        <Header
                            totalPrice="1 432 ₽"
                            originalPrice="2 000 ₽"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('childrenTitle', () => {
                const tree = renderer
                    .create(
                        <Header
                            totalPrice="2 437 ₽"
                            childrenTitle="ООО «Кусочек лета»"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('childrenSubtitle', () => {
                const tree = renderer
                    .create(
                        <Header
                            totalPrice="232 ₽"
                            childrenSubtitle="Заказ №9031"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('childrenAfter', () => {
                const tree = renderer
                    .create(
                        <Header
                            totalPrice="10 ₽"
                            childrenAfter={(
                                <div>Logo</div>
                            )}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <Header
                            totalPrice="100 ₽"
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
