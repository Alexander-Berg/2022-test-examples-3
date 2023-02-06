describe('SearchPage / Темная тема', function() {
    describe('Шапка', async function() {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone&promo=nomooa&exp_flags=dark_theme_desktop=dark');
            await bro.yaWaitForVisible('.Header', 3000, 'Шапка не появилась');
        });

        describe('Внешний вид', async function() {
            it('Лого', async function() {
                const bro = this.browser;

                await bro.yaWaitForVisible('.serp-header__logo', 3000, 'Шапка не появилась');
                await bro.assertView('logo', '.serp-header__logo');
            });
        });
    });
});
