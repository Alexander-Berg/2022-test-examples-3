import * as React from 'react';
import { DecoratorFunction } from '@storybook/client-api';
import { StoryFnReactReturnType } from '@storybook/react/dist/ts3.9/client/preview/types';

export const withSuspense: DecoratorFunction<StoryFnReactReturnType> = (story) => {
    return <React.Suspense fallback={null}>{story()}</React.Suspense>;
};
