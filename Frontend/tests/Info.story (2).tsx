import React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { withStubReduxProvider } from '@src/storybook/decorators/withStubReduxProvider';
import { Info } from '../index';

createPlatformStories('Tests/MainPage-Info', Info, stories => {
    stories
        .addDecorator(withStubReduxProvider())
        .add('plain', Info => <Info />);
});
