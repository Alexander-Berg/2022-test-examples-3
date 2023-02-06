const COOKIE_NAME = 'font_loaded';

specs({
    feature: 'YandexFont',
}, function() {
    hermione.only.notIn('safari13');
    it('Проверка подгрузки YandexSans шрифта', async function() {
        const browser = this.browser;
        await browser.url('/turbo?stub=productspage/tv-breadcrumbs.json')
            .yaWaitForVisible(PO.page())
            .getCookie().then(function(cookie) {
                const hasCookie = cookie.some(item => item.name === COOKIE_NAME);
                assert.isFalse(hasCookie, `cookie с именем ${COOKIE_NAME} установлена`);
            })
            .url('/turbo?stub=productspage/tv-breadcrumbs.json&exp_flags=yandex-font')
            .yaWaitForVisible(PO.page());
        await browser.yaWaitUntil(`cookie с именем ${COOKIE_NAME} не установлена.`,
            () => browser.getCookie().then(function(cookie) {
                return cookie.some(item => item.name === COOKIE_NAME);
            }));
        await browser.assertView('plain', PO.page.result());
    });
});
