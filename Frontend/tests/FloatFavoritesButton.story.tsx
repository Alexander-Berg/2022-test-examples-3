import React from 'react';

import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { FloatFavoritesButton } from '../index';

import './FloatFavoritesButton.story.scss';

createPlatformStories<{}>('Tests/FloatFavoritesButton', FloatFavoritesButton, stories => {
    stories
        .addDecorator(withStaticRouter())
        .add('plain', FloatFavoritesButton => (
            <FloatFavoritesButton
            />
        ));
});
