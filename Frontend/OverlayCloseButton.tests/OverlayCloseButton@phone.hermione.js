specs({
    feature: 'Кнопка закрытия оверлея в шапке',
}, function() {
    hermione.only.notIn('safari13');
    it('Не меняет шапку без cgi-параметра overlay-drawer=1', async function() {
        const browser = this.browser;

        await browser.url('/turbo?stub=overlayclosebutton/with-menu.json');
        await browser.yaShouldNotBeVisible(PO.headerSticky());
        await browser.assertView('default', PO.header());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид по умолчанию', async function() {
        const browser = this.browser;

        await browser.url('/turbo?stub=overlayclosebutton/default.json&overlay-drawer=1');
        await browser.assertView('default', PO.headerSticky());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с меню', async function() {
        const browser = this.browser;

        await browser.url('/turbo?stub=overlayclosebutton/with-menu.json&overlay-drawer=1');
        await browser.assertView('with-menu', PO.headerSticky());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид wiki', async function() {
        const browser = this.browser;

        await browser.url('/turbo/ru.wikipedia.org/s/?stub=overlayclosebutton/wiki.json&overlay-drawer=1');
        await browser.assertView('wiki', PO.headerSticky());
    });
});
