import React from 'react';
import renderer from 'react-test-renderer';

import {
    cnRadioGroup,
    RadioGroupDesktop,
    RadioGroupTouchPhone,
} from '.';

describe('RadioGroup', () => {
    test('cnRadioGroup', () => {
        expect(cnRadioGroup()).toBe('YpcRadioGroup');
    });

    for (const [platform, RadioGroup] of Object.entries({
        desktop: RadioGroupDesktop,
        'touch-phone': RadioGroupTouchPhone,
    })) {
        describe(platform, () => {
            test('value & options', () => {
                const tree = renderer
                    .create(
                        <RadioGroup
                            value="v2"
                            options={[
                                { value: 'v1', children: 'Item 1', disabled: true },
                                { value: 'v2', children: (<div>Item 2</div>) },
                                { value: 'v3', children: 'Item 3', id: 'test-id' },
                            ]}
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('name', () => {
                const tree = renderer
                    .create(
                        <RadioGroup
                            options={[
                                { value: 'v1', children: 'Item 1' },
                                { value: 'v2', children: 'Item 2' },
                            ]}
                            name="test-control-name"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });

            test('className', () => {
                const tree = renderer
                    .create(
                        <RadioGroup
                            options={[]}
                            className="Test"
                        />
                    )
                    .toJSON();

                expect(tree).toMatchSnapshot();
            });
        });
    }
});
