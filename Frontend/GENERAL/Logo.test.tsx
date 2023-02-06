import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnLogo,
    LogoDesktop,
    LogoTouchPhone,
} from '.';

describe('Logo', () => {
    test('cnLogo', () => {
        expect(cnLogo()).toBe('YpcLogo');
    });

    for (const [platform, Logo] of Object.entries({
        desktop: LogoDesktop,
        'touch-phone': LogoTouchPhone,
    })) {
        describe(platform, () => {
            test('className', () => {
                const tree = renderer
                    .create(
                        <Logo
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
