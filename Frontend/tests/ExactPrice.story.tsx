import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Platform } from '@src/typings/platform';
import type { IExactPriceProps } from '../ExactPrice.typings';
import { ExactPrice } from '../index';

const defaultProps: IExactPriceProps = {
    price: {
        type: 'exact',
        current: '3590',
        old: '3990',
        currency: 'RUR',
    },
};

createPlatformStories('Tests/ExactPrice', ExactPrice, (stories, platform) => {
    const size: React.CSSProperties = platform === Platform.Desktop ? { width: 100 } : { width: 80 };
    stories
        .add('plain', ExactPrice => (
            <ExactPrice {...defaultProps} />
        ))
        .add('small container', ExactPrice => (
            <div style={size}>
                <ExactPrice {...defaultProps} />
            </div>
        ))
        .add('big current price', ExactPrice => (
            <div style={size}>
                <ExactPrice
                    price={{
                        ...defaultProps.price,
                        current: '14000000',
                    }}
                />
            </div>
        ))
        .add('revert', ExactPrice => (
            <ExactPrice
                {...defaultProps}
                revert
            />
        ));
});
