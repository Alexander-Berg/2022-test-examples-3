import * as React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { RatingHistrogram } from '..';

createPlatformStories('Tests/RatingHistrogram', RatingHistrogram, stories => {
    const size: React.CSSProperties = { width: 315 };
    const props = {
        rating: 4.5,
        max: 5,
        step: 0.5,
        count: 1921,
        counts: [1103, 300, 22, 0, 203],
    };

    stories.add('showcase', RatingHistrogram => {
        return (
            <div style={size}>
                <RatingHistrogram {...props} />
            </div>
        );
    });
});
