import type { Args } from '@storybook/addons';
import { toId } from '@storybook/csf';

export async function yaOpenComponent(
    this: WebdriverIO.Browser,
    component: string,
    story: string,
    withPlatform: boolean = false,
    args: Args = {},
) {
    const platform = await this.getMeta('platform');
    const componentWithPlatform = [
        component,
        withPlatform && platform,
    ].join('-');
    const storyId = toId(componentWithPlatform, story);

    return this.selectStory(storyId, args);
}
