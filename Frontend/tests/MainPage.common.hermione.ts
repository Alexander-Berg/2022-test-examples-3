describe('MainPage', function() {
    const categoriesListSelector = '.CategoriesList';
    const tenthCategorySelector = '.CategoriesList-Item:nth-child(10)';
    const productCardsListSelector = '.ProductCardsList';
    const guruSelector = '.GuruCategories';

    // Достаточно проверить в одном браузере.
    hermione.only.in(['linux-chrome', 'linux-chrome-iphone']);
    it('Сохранение позиции скролла', async function() {
        const { browser } = this;

        await browser.yaOpenPageByUrl('/products?promo=nomooa');
        await browser.yaWaitForVisible(categoriesListSelector);

        await browser.yaScroll(tenthCategorySelector);
        const savedScrollTop = await browser.execute(() => document.documentElement.scrollTop);

        await browser.click(tenthCategorySelector);
        await browser.yaWaitForVisible(productCardsListSelector);
        await browser.yaScroll(5000);

        await browser.back();
        await browser.yaWaitForVisible(categoriesListSelector);

        const newScrollTop = await browser.execute(() => document.documentElement.scrollTop);

        assert.equal(savedScrollTop, newScrollTop, 'Позиция скролла не сохранилась');
    });

    function getPosition(browser: WebdriverIO.Browser, selector: string) {
        return browser.execute(param => document.querySelector(param)?.getBoundingClientRect().y ?? 0, selector);
    }

    it('Отображение списка категорий', async function() {
        const { browser } = this;

        await browser.yaOpenPageByUrl('/products');
        await this.browser.yaWaitForVisible(categoriesListSelector);
        await this.browser.yaAssertViewportView('plain');
    });

    it('Гуру вверху', async function() {
        const { browser } = this;

        await browser.yaOpenPageByUrl('/products?exp_flags=GARAGE_alice-guru=1&exp_flags=enable_guru_saas_request&is_guru_first=1');

        const guruPosition = await getPosition(browser, guruSelector);
        const recommenderPosition = await getPosition(browser, categoriesListSelector);

        assert.isTrue(guruPosition < recommenderPosition, 'Гуру ниже');
    });

    it('Гуру внизу', async function() {
        const { browser } = this;

        await browser.yaOpenPageByUrl('/products?exp_flags=GARAGE_alice-guru=1&exp_flags=enable_guru_saas_request');

        const recommenderPosition = await getPosition(browser, categoriesListSelector);
        const guruPosition = await getPosition(browser, guruSelector);

        assert.isTrue(guruPosition > recommenderPosition, 'Гуру выше');
    });
});
