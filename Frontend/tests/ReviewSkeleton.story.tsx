import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { ReviewSkeleton } from '..';

createPlatformStories('Tests/Reviews/ReviewSkeleton', ReviewSkeleton, stories => {
    stories
        .add('plain', ReviewSkeleton => {
            return (
                <div style={{ width: 300 }}>
                    <ReviewSkeleton />
                </div>
            );
        });
});
