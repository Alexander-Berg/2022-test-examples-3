import { DecoratorFunction } from '@storybook/client-api';
import { StoryFnReactReturnType } from '@storybook/react/dist/ts3.9/client/preview/types';

import { LS_FLAGS } from 'constants/constants';

export const withFlags: DecoratorFunction<StoryFnReactReturnType> = (story, { parameters }) => {
    localStorage.clear();

    if (parameters.flags) {
        localStorage.setItem(LS_FLAGS, JSON.stringify(parameters.flags));
    }

    return story();
};
