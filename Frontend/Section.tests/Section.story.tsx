import * as React from 'react';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { Section } from '../index';

createPlatformStories('Tests/Section', Section, stories => {
    stories
        .add('showcase', Section => {
            return (
                <Section />
            );
        });
});
