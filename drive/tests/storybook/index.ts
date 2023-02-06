import { Args as DefaultArgs } from '@storybook/addons';
import { Story } from '@storybook/react';

export type BaseStoryHermioneFn = (
    browser: WebdriverIO.Browser,
    selector: string,
) => Promise<WebdriverIO.Browser> | Promise<void>;

export interface BaseStoryRouteProps {
    query?: Dict;
}

export type BaseStory<Args = DefaultArgs> = Story<Args & BaseStoryRouteProps> & {
    hermioneFn?: Optional<Nullable<BaseStoryHermioneFn>>;
    hermionePlainFn?: Optional<Nullable<BaseStoryHermioneFn>>;
    hermioneSelector?: Optional<string>;
};
