describe('ProductPage / Темная тема', function() {
    describe('Темная тема', async function() {
        describe('Внешний вид', async function() {
            describe('Шапка', async function() {
                beforeEach(async function() {
                    const bro = this.browser;
                    await bro.yaOpenPageByUrl('/products/product/1441144417/sku/101465383818?promo=nomooa&exp_flags=dark_theme_desktop=dark');
                    await bro.yaWaitForVisible('.Header', 3000, 'Шапка не появилась');
                });

                it('Лого', async function() {
                    const bro = this.browser;

                    await bro.yaWaitForVisible('.serp-header__logo', 3000, 'Шапка не появилась');
                    await bro.assertView('logo', '.serp-header__logo');
                });
            });
        });
    });
});
