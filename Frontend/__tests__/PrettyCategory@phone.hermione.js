describe('PrettyCategory', function() {
    it('Внешний вид и переход в категорию', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            // решили написать тест на реальных данных, так как поймали баг
            // который не воспроизводился на пропатченных данных
            // @see TURBOUI-6511
            service: 'tvoisadrus.ru',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.EcomScreen_type_main');

        await browser.yaWaitForVisible('.PrettyCategory');
        await browser.assertView('plain', '.PrettyCategory');

        await browser.click('.PrettyCategory');
        await browser.yaWaitForVisible('.EcomScreen_type_product-list');
        await browser.yaWaitForHidden('.Skeleton');
        await browser.yaShouldBeVisible('.ProductList');

        const url = await browser.getUrl();
        const { searchParams, pathname } = new URL(url);

        assert.strictEqual(
            pathname,
            '/turbo/tvoisadrus.ru/n/yandexturbocatalog/',
            'Произошел переход не на страницу категории',
        );

        assert.strictEqual(
            searchParams.get('category_id'),
            '10',
            'Произошел переход на некорректную категорию',
        );
    });

    it('Внешний вид со скидкой и описанием', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            query: {
                patch: 'addPrettyCategory',
                prettyCategory: 'sales,description',
            },
        });

        await browser.yaWaitForVisible('.EcomScreen_type_main');

        await browser.yaWaitForVisible('.PrettyCategory');
        await browser.assertView('plain', '.PrettyCategory');
    });
});
