import type { TPlatformStorybook } from '@src/storybook/utils/createPlatformStories';

interface IOptionsAssertThemeStorybook extends Hermione.AssertViewOpts {
    platforms?: TPlatformStorybook[];
}

// Делает скрины по селектору с разными темами в сторибуке
export async function yaAssertViewThemeStorybook(
    this: WebdriverIO.Browser,
    title: string,
    selector: string | string[],
    options: IOptionsAssertThemeStorybook = {},
) {
    await this.assertView(title + '_light', selector);

    if (options?.platforms) {
        const platform = await this.getMeta('platform') as TPlatformStorybook;

        if (!options.platforms.includes(platform)) {
            return;
        }
    }

    await this.yaChangeThemeStorybook('dark');
    await this.pause(300);
    await this.assertView(title + '_dark', selector);
    await this.yaChangeThemeStorybook('light');
    await this.pause(300);
}
