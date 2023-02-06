import { DecoratorFunction } from '@storybook/client-api';
import { StoryFnReactReturnType } from '@storybook/react/dist/ts3.9/client/preview/types';
import mockdate from 'mockdate';

export const withDate: DecoratorFunction<StoryFnReactReturnType> = (story, { parameters }) => {
    mockdate.reset();

    if (parameters.date) {
        mockdate.set(parameters.date);
    }

    return story();
};
