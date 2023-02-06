async function openDrawer(browser) {
    await browser.url('?&text=ru.wikipedia.org&stub=wikilanguages%2Fdefault.json');
    await browser.yaWaitForVisible(PO.page());
    await browser.yaWaitForVisible(PO.header());
    await browser.yaWaitForVisible(PO.blocks.wikilanguageButton());

    await browser.click(PO.blocks.wikilanguageButton());
    await browser.yaWaitForVisible(PO.blocks.drawer(), 'Не открылся поп-ап со списком языков');
}

specs({
    feature: 'WikiLanguages',
}, () => {
    hermione.only.notIn('safari13');
    it('Кнопка языков на странице Wikipedia', async function() {
        const browser = this.browser;

        await browser.url('?&text=ru.wikipedia.org&stub=wikilanguages%2Fdefault.json');
        await browser.yaWaitForVisible(PO.page());
        await browser.yaWaitForVisible(PO.header());

        await browser.assertView('wikipedia', PO.header());
    });

    hermione.only.notIn('safari13');
    it('Кнопка языков не отображается на страницах паблишеров', async function() {
        const browser = this.browser;
        const isExist = false;

        await browser.url('?text=test_news');
        await browser.yaWaitForVisible(PO.page());
        await browser.yaWaitForVisible(PO.header());

        const { value: button } = await this.browser.execute(function() {
            return Boolean(document.querySelector('.turbo-wikilanguages__button'));
        });

        assert.equal(button, isExist, 'Кнопка переключения языков отображается в шапке');
    });

    hermione.only.notIn('safari13');
    it('Флаг отключения убирает кнопку переключения языков', async function() {
        const browser = this.browser;
        const isExist = false;

        await browser.url('?&text=ru.wikipedia.org&stub=wikilanguages%2Fdefault.json&exp_flags=disable-wikilanguages=1');
        await browser.yaWaitForVisible(PO.page());
        await browser.yaWaitForVisible(PO.header());

        const { value: button } = await this.browser.execute(function() {
            return Boolean(document.querySelector('.turbo-wikilanguages__button'));
        });

        assert.equal(button, isExist, 'Кнопка переключения языков отображается в шапке');
    });

    describe('Drawer', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид', async function() {
            const browser = this.browser;
            await openDrawer(browser);

            await browser.yaAssertViewportView('drawer');
        });

        hermione.only.notIn('safari13');
        it('Скрывается по клику на текущий язык', async function() {
            const browser = this.browser;
            await openDrawer(browser);

            await browser.click(PO.blocks.wikilanguageItem());
            await browser.yaShouldNotBeVisible(PO.blocks.drawer());
        });

        hermione.only.notIn('safari13');
        it('Скрывается и открывает новую страницу', async function() {
            const browser = this.browser;
            await openDrawer(browser);

            await browser.click(PO.blocks.wikilanguageItemSecond());
            await browser.yaShouldNotBeVisible(PO.blocks.drawer());

            const { value } = await this.browser.execute(function() { return location.href });
            assert.include(value, 'https://ar.m.wikipedia.org');
        });
    });
});
