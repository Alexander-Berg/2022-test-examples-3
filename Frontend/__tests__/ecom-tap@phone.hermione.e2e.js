describe('Ecom-tap', function() {
    it('Сервис открывается', function() {
        return this.browser
            .yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
            })
            .yaWaitForVisible('.EcomScreen_type_product-list');
    });

    hermione.skip.in('safari11', 'TURBOUI-3758');
    it('Открытие страницы листинга', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'super01.ru',
            pageType: 'catalog',
            query: { category_id: 13292 },
        });

        await browser.yaWaitForVisible('.EcomScreen');
        await browser.yaWaitForVisible('.ProductList-Content');

        const initialItems = await browser.elements('.ProductItem');
        const initialCount = initialItems.value.length;

        await browser.execute(function() {
            // Скроллим вниз
            window.scroll(
                0,
                document.querySelector('.ProductList-Content').offsetHeight
            );
        });

        // Ждем подгрузки товаров
        await browser.yaWaitForHidden('.ProductItem_skeleton');

        const resultItems = await browser.elements('.ProductItem');
        const resultCount = resultItems.value.length;

        assert.isAbove(resultCount, initialCount, 'Товары не подгрузились');
    });

    hermione.skip.in('safari11', 'TURBOUI-3758');
    describe('Главная', () => {
        it('Список категорий', function() {
            return this.browser
                .yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                })
                .yaWaitForVisible('.CategoryList', 'Отсутствует список категорий');
        });
        it('Баннер', function() {
            return this.browser
                .yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                })
                .yaWaitForVisible('.EcomPromo', 'Отсутствует баннер');
        });
        it('Товары со скидками', function() {
            return this.browser
                .yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                })
                .yaWaitForVisible('.Collection .DiscountBadge', 'Отсутствует блок товаров со скидками');
        });
        it('Горизонтальные галереи товаров', function() {
            return this.browser
                .yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                })
                .yaWaitForVisible('.EcomScreen_type_main')
                .elements('.ProductList_type_horizontal')
                .then(elements => { assert.equal(elements.value.length, 2, 'Не загрузились горизонтальные галереи товаров') });
        });
    });
});
