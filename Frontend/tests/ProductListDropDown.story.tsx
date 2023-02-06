import React from 'react';
import { withKnobs, select } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import type { IProductListControlsButtonProps } from '@src/components/ProductListControlsButton/ProductListControlsButton.typings';
import { ProductListDropDown } from '..';

const valueMap = {
    first: { text: 'First' },
    second: { text: 'Second' },
    third: { text: 'Third' },
};
const values = Object.keys(valueMap);
const value = values[0];

const viewOptions: Record<string, IProductListControlsButtonProps['view']> = {
    Clear: 'clear',
    Default: 'default',
};

createPlatformStories('Tests/ProductListDropDown', ProductListDropDown, stories => {
    stories
        .addDecorator(withKnobs)
        .add('plain', ProductListDropDown => (
            <ProductListDropDown
                value={value}
                values={values}
                valueMap={valueMap}
                iconRight="arrow"
                view={select('View', viewOptions, 'default')}
            />
        ));
});
