import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnButton,
    ButtonDesktop,
    ButtonTouchPhone,
} from '.';

describe('Button', () => {
    test('cnButton', () => {
        expect(cnButton()).toBe('YpcButton');
    });

    for (const [platform, Button] of Object.entries({
        desktop: ButtonDesktop,
        'touch-phone': ButtonTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <Button className="Test">
                            Text
                        </Button>
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('disabled', () => {
                const tree = renderer
                    .create(
                        <Button disabled>
                            <span>Left</span>
                            <span>Right</span>
                        </Button>
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
