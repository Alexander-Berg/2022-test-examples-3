import { THEME_NAME, BACKGROUNDS_STORYBOOK } from '@src/constants/common';

const ALL_THEME_CLASSES = Array.from(Object.values(THEME_NAME).reduce((acc, item) => {
    item.split(' ').forEach(cls => acc.add(cls));
    return acc;
}, new Set<string>()));

// В storybook изменяет цвет фона
export function yaChangeThemeStorybook(this: WebdriverIO.Browser, theme: string) {
    return this.execute(function(
        theme: string,
        backgrounds: Record<string, string>,
        THEME_NAME: Record<string, string>,
        ALL_THEME_CLASSES: string[],
    ) {
        document.documentElement.style.backgroundColor = backgrounds[theme];

        const root = document.querySelector('#root-theme');
        if (root) {
            root.classList.remove(...ALL_THEME_CLASSES);
            root.classList.add(...THEME_NAME[theme].split(' '));
        }
    }, theme, BACKGROUNDS_STORYBOOK, THEME_NAME, ALL_THEME_CLASSES);
}
