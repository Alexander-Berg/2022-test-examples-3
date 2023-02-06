import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnBindingForm,
    BindingFormDesktop,
    BindingFormTouchPhone,
} from '.';

describe('BindingForm', () => {
    test('cnBindingForm', () => {
        expect(cnBindingForm()).toBe('YpcBindingForm');
    });

    for (const [platform, BindingForm] of Object.entries({
        desktop: BindingFormDesktop,
        'touch-phone': BindingFormTouchPhone,
    })) {
        describe(platform, () => {
            test('default', () => {
                const tree = renderer
                    .create(
                        <BindingForm
                            dieHardUrl="https://diehard.yandex.ru/web/card_form"
                            purchaseToken="yandex-pay"
                            isPaymentButton
                            isPaymentButtonDisabled
                            BeforeDieHardFrame={() => (
                                <div>BeforeDieHardFrame</div>
                            )}
                            BeforePaymentButton={() => (
                                <div>BeforePaymentButton</div>
                            )}
                        >
                            children
                        </BindingForm>
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
