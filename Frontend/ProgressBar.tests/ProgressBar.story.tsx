import * as React from 'react';
import { number } from '@storybook/addon-knobs';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { ProgressBar } from '../index';

createPlatformStories('Tests/ProgressBar', ProgressBar, stories => {
    stories
        .add('showcase', ProgressBar => {
            const value = number('Value', 10);

            return (
                <ProgressBar value={value} />
            );
        });
});
