import React from 'react';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { NotFound } from '../index';

createPlatformStories<{}>('Tests/NotFound', NotFound, stories => {
    stories
        .add('plain', NotFound => (
            <NotFound
            />
        ));
});
