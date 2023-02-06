import React from 'react';
import { action } from '@storybook/addon-actions';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { ReviewsEmptyList } from '..';

createPlatformStories('Tests/Reviews/ReviewsEmptyList', ReviewsEmptyList, stories => {
    stories
        .add('plain', ReviewsEmptyList => {
            return (
                <div style={{ width: 300, border: '1px dotted #666' }}>
                    <ReviewsEmptyList />
                </div>
            );
        })
        .add('error', ReviewsEmptyList => {
            return (
                <div style={{ width: 300, border: '1px dotted #666' }}>
                    <ReviewsEmptyList error onRetryClick={action('onRetryClick')} />
                </div>
            );
        });
});
