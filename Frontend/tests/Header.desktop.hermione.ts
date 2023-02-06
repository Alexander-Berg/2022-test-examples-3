import { ETheme } from '@src/store/services/internal/types';
import type { mockData } from '../../../../tests/hermione/commands/yaMockSuggest';

hermione.only.in('linux-chrome');

const themes: ETheme[] = [ETheme.LIGHT, ETheme.DARK];

const mock: mockData = [
    ['fulltext', 'iphone 13 pro', { personal: true }],
    ['fulltext', 'iphone 11 256', {}],
    ['fulltext', 'iphone 13 pro max', {}],
    ['fulltext', 'iphone 12 pro', {}],
    ['fulltext', 'iphone', {}],
];

const assertSuggestView = async function(browser: WebdriverIO.Browser, name: string) {
    await browser.assertView(name, ['.mini-suggest', '.mini-suggest__popup-content']);
};

describe('Саджест', function() {
    const query = 'iphone';

    describe('Внешний вид', function() {
        themes.forEach(serviceTheme => {
            themes.forEach(suggestTheme => {
                it(`Сервисная тема ${serviceTheme}, тема саджеста ${suggestTheme}`, async function() {
                    await this.browser.yaOpenPageByUrl(`/products?text=${query}&exp_flags=dark_theme_desktop=${serviceTheme};suggest_theme_color=${suggestTheme}`);
                    await this.browser.yaMockSuggest(query, mock);
                    await this.browser.click('.mini-suggest__input');
                    await this.browser.yaWaitForVisible('.mini-suggest__popup-content .mini-suggest__arrow', 'Саджест не появился');
                    await assertSuggestView(this.browser, 'plain');
                });
            });
        });
    });
});
