import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { PriceRange } from '../index';
import type { IPriceRangeProps } from '../PriceRange.typings';

const defaultProps: IPriceRangeProps = {
    range: {
        min: '1232',
        max: '2132',
        currency: 'RUR',
        type: 'range',
    },
};

createPlatformStories('Tests/PriceRange', PriceRange, stories => {
    stories
        .add('plain', PriceRange => (
            <PriceRange {...defaultProps} />
        ))
        .add('small container', PriceRange => (
            <div style={{ width: 60 }}>
                <PriceRange {...defaultProps} />
            </div>
        ));
});
