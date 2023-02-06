import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { PriceValue } from '../index';
import type { IPriceValueProps } from '../PriceValue.typings';

const defaultProps: IPriceValueProps = {
    value: 27_990,
};

createPlatformStories('Tests/PriceValue', PriceValue, stories => {
    stories
        .add('plain', Price => (
            <Price {...defaultProps} />
        ));
});
