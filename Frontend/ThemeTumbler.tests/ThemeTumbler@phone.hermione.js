specs({
    feature: 'Кнопка переключения темы',
}, () => {
    const params = 'patch=customTheme';
    const defaultLightUrl = '/turbo?stub=themetumbler/default-light.json';
    const defaultDarkUrl = '/turbo?stub=themetumbler/default-dark.json';
    const pageUrl = '/turbo/tvc.ru/s/news/show/id/201318/?' + params;

    const themeDarkClass = 'page_custom-display-theme_dark';
    const themeLightClass = 'page_custom-display-theme_light';

    async function setCookie(name, value, browser) {
        await browser.execute(function(name, value) {
            let updatedCookie = encodeURIComponent(name) + '=' + encodeURIComponent(value);
            document.cookie = updatedCookie;
        }, name, value);
    }

    describe('Светлая тема (light)', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид кнопки переключения темы', function() {
            return this.browser
                .url(defaultLightUrl)
                .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
                .yaWaitForVisible(PO.blocks.themeTumbler(), 'Кнопка не появилась')
                .assertView('snippet-light', PO.blocks.themeTumbler());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид кнопки переключения темы на странице и после нажатия', async function() {
            await this.browser.url(pageUrl);

            await setCookie('turbopage-theme', 'light', this.browser);

            await this.browser.refresh();
            await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась после перезагрузки');

            await this.browser.click(PO.blocks.sandwichMenu.handler());
            await this.browser.assertView('snippet-menu-light', PO.blocks.sandwichMenuContainer.contentWrap());
            await this.browser.click(PO.blocks.themeTumbler.button());
            const className = await this.browser.getAttribute(PO.blocks.page(), 'class');
            assert.isTrue(className.includes(themeDarkClass));
        });
    });

    describe('Темная тема (dark)', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид кнопки переключения темы', function() {
            return this.browser
                .url(defaultDarkUrl)
                .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
                .execute(function() {
                    document.querySelector('.page').classList.add(['page_custom-display-theme_dark']);
                })
                .yaWaitForVisible(PO.blocks.themeTumbler(), 'Кнопка не появилась')
                .assertView('snippet-dark', PO.blocks.themeTumbler());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид кнопки переключения темы на странице и после нажатия', async function() {
            await this.browser.url(pageUrl);

            await setCookie('turbopage-theme', 'dark', this.browser);

            await this.browser.refresh();
            await this.browser.yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась после перезагрузки');

            await this.browser.click(PO.blocks.sandwichMenu.handler());
            await this.browser.assertView('snippet-menu-dark', PO.blocks.sandwichMenuContainer.contentWrap());
            await this.browser.click(PO.blocks.themeTumbler.button());
            const className = await this.browser.getAttribute(PO.blocks.page(), 'class');
            assert.isTrue(className.includes(themeLightClass));
        });
    });

    hermione.only.notIn('safari13');
    it('Переключение иконки в шапке', async function() {
        const { browser } = this;

        await browser.url('/turbo?stub=themetumbler/page.json');
        await browser.yaWaitForVisible(PO.page());
        await browser.execute(function() {
            document.querySelector('.page').classList.remove('page_custom-display-theme_light');
            document.querySelector('.page').classList.remove('page_custom-display-theme_dark');
        });
        const { value: isLightLogoFirst } = await browser.execute(function() {
            return document.querySelector('.header .turbo-image').getAttribute('src').includes('svg');
        });
        assert.equal(isLightLogoFirst, true);
        await browser.click(PO.blocks.sandwichMenu.handler());
        await browser.yaWaitForVisible(PO.blocks.sandwichMenuContainer.content());
        await browser.yaTouchScroll(PO.blocks.sandwichMenuContainer.contentWrap(), 0, 100);
        await browser.yaWaitForVisible(PO.blocks.themeTumbler());
        await browser.click(PO.themeTumblerButton());
        await browser.click(PO.blocks.sandwichMenuContainer.closeButton());
        await browser.yaShouldNotBeVisible(PO.blocks.sandwichMenuContainer.content());
        const { value: isDarkLogo } = await browser.execute(function() {
            return document.querySelector('.header .turbo-image').getAttribute('src').includes('logo_horizontal_s_x20');
        });
        assert.equal(isDarkLogo, true);
        await browser.click(PO.blocks.sandwichMenu.handler());
        await browser.yaWaitForVisible(PO.blocks.sandwichMenuContainer.content());
        await browser.yaTouchScroll(PO.blocks.sandwichMenuContainer.contentWrap(), 0, 300);
        await browser.click(PO.themeTumblerButton());
        await browser.click(PO.blocks.sandwichMenuContainer.closeButton());
        await browser.yaShouldNotBeVisible(PO.blocks.sandwichMenuContainer.content());
        const { value: isLightLogoSecond } = await browser.execute(function() {
            return document.querySelector('.header .turbo-image').getAttribute('src').includes('svg');
        });
        assert.equal(isLightLogoSecond, true);
    });
});
