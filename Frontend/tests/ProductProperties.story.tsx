import React from 'react';
import { boolean, number, text } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { createProductPropertiesStub } from '../../../storybook/stubs/entitites/createProductStub';
import { ProductProperties } from '../index';

const [STUB_PROPERTY] = createProductPropertiesStub(undefined, 1);

export const ProductPropertiesKnobs = () => {
    const count = number('count', 3);
    const name = text('firstPropertyName', STUB_PROPERTY.name);
    const value = text('firstPropertyValue', STUB_PROPERTY.value);

    const properties = createProductPropertiesStub({ name, value }, count);
    const longProp = createProductPropertiesStub({
        name: 'Длинное поле',
        value: new Array(5).fill('очень длинное поле').join(' '),
    });

    const posLongProp = number('Position long prop', -1);

    if (posLongProp >= 0) {
        properties.splice(posLongProp, 0, longProp[0]);
    }

    return { properties };
};

createPlatformStories('Share/ProductProperties', ProductProperties, stories => {
    stories
        .add('default', ProductProperties => {
            const { properties } = ProductPropertiesKnobs();
            const advanced = boolean('Advanced', false);

            return (
                <div style={{ maxWidth: '400px' }}>
                    <ProductProperties properties={properties} view={advanced ? 'advanced' : 'default'} />
                </div>
            );
        });
});
