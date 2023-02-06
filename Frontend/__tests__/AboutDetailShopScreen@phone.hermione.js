describe('AboutDetailShopScreen', () => {
    it('Внешний вид', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/n/yandexturbocatalog/about_detail/',
            query: { about_category_id: 0 },
        });
        await browser.assertView('plain', '.AboutDetailShopScreen');
    });

    it('Внешний вид с длинными текстами', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/n/yandexturbocatalog/about_detail/',
            query: { about_category_id: 0, patch: 'detailLongText' },
        });
        await browser.assertView('plain', '.AboutDetailShopScreen');
    });
});
