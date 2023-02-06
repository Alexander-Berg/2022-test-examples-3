import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnEmailInput,
    EmailInputDesktop,
    EmailInputTouchPhone,
} from '.';

describe('EmailInput', () => {
    test('cnEmailInput', () => {
        expect(cnEmailInput()).toBe('YpcEmailInput');
    });

    for (const [platform, EmailInput] of Object.entries({
        desktop: EmailInputDesktop,
        'touch-phone': EmailInputTouchPhone,
    })) {
        describe(platform, () => {
            test('value', () => {
                const tree = renderer
                    .create(
                        <EmailInput
                            value="example@yandex.ru"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <EmailInput
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
