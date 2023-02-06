/// <reference types="hermione" />
import { Meta } from '@storybook/react';

import { StorybookContainerProps } from 'tests/storybook/helpers/createStorybookTemplate';

export type StoryMeta<T> = Meta<T & StorybookContainerProps> & {
    hermioneSelector?: Optional<string> | Optional<string>[];
    hermioneOptions?: Hermione.AssertViewOpts;
};

export function createStorybookMeta<T>({ args, ...restMeta }: StoryMeta<T>): StoryMeta<T> {
    return { args, ...restMeta } as StoryMeta<T>;
}
