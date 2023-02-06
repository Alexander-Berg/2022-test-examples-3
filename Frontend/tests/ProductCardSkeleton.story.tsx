import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Platform } from '@src/typings/platform';
import { ProductCardSkeleton } from '../index';

createPlatformStories('Tests/ProductCardSkeleton', ProductCardSkeleton, (stories, platform) => {
    const wrapperStyle: React.CSSProperties = platform === Platform.Desktop ? { width: 250 } : { width: 168 };
    stories
        .add('plain', ProductCardLoader => (
            <div style={wrapperStyle}>
                <ProductCardLoader />
            </div>
        ));
});
