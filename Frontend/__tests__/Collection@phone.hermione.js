describe('Collection', function() {
    it('Подгружаются товары по кнопке', async function() {
        const browser = this.browser;
        await browser.yaOpenEcomSpa({
            service: 'ymturbo.t-dir.com',
            pageType: 'main',
        });
        function getCollectionLength() { return document.querySelectorAll('.Link.Collection-Item').length }
        await browser.yaWaitForVisible('.Collection-MoreButton_status_has-more', 'Отсутствует кнопка Показать еще');
        await browser.yaScrollPage('.Collection-MoreButton_status_has-more', 0.3);
        const { value: initialCollectionLength } = await browser.execute(getCollectionLength);
        await browser.click('.Collection-MoreButton_status_has-more');
        await browser.yaWaitForHidden('.Collection-MoreButton_status_loading', 'Не исчезла анимация кнопки Показать еще');
        const { value: collectionLengthAfterLoad } = await browser.execute(getCollectionLength);
        assert.isTrue(collectionLengthAfterLoad > initialCollectionLength, 'Товары не подгрузились');
    });

    it('Переход в категорию', async function() {
        const browser = this.browser;
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });
        await browser.getText('.Collection .Title')
            .then(text => assert.equal(text, 'Товары со скидками', 'Нет блока с названием "Товары со скидками"'));
        const collectionItemTitle = await browser.getText('.Collection .Collection-Item:nth-of-type(1) .Collection-ItemInfo p');
        await browser.yaScrollPage('.Collection .Collection-Item:nth-of-type(1)', 0.3);
        await browser.click('.Collection .Collection-Item:nth-of-type(1)');
        await browser.yaWaitForVisible('.EcomScreen');
        await browser.yaWaitForVisible('.ProductList-Content', 'Не загрузился каталог');
        const listTitle = await browser.getText('.EcomScreen_type_product-list .Title');
        assert.strictEqual(collectionItemTitle, listTitle, 'Заголовок страницы не соответствует заголовку категорийной карточки');
        const itemsList = await browser.elements('.ProductItem');
        const itemsCount = itemsList.value.length;
        const discount = await browser.elements('.ProductItem .DiscountBadge_type_tilted');
        const discountCount = discount.value.length;
        assert.equal(itemsCount, discountCount);
    });

    it('После перехода не схлопываются', async function() {
        const browser = this.browser;
        await browser.yaOpenEcomSpa({
            service: 'ymturbo.t-dir.com',
            pageType: 'main',
        });
        function getCollectionLength() { return document.querySelectorAll('.Link.Collection-Item').length }
        await browser.yaWaitForVisible('.Collection-MoreButton_status_has-more', 'Отсутствует кнопка Показать еще');
        await browser.yaScrollPage('.Collection-MoreButton_status_has-more', 0.3);
        await browser.click('.Collection-MoreButton_status_has-more');
        await browser.yaWaitForHidden('.Collection-MoreButton_status_loading', 'Не исчезла анимация кнопки Показать еще');
        const { value: collectionCount } = await browser.execute(getCollectionLength);
        await browser.click('.Collection .Collection-Item:nth-of-type(1)');
        await browser.yaWaitForVisible('.EcomScreen');
        await browser.yaWaitForVisible('.ProductList-Content', 'Не загрузился каталог');
        await browser.click('.ScreenHeaderBack');
        await browser.yaWaitForVisible('.Link.Collection-Item', 'Нет карточек в блоке "Популярные категории"');
        const { value: collectionCountAfterReturn } = await browser.execute(getCollectionLength);
        assert.equal(collectionCount, collectionCountAfterReturn, 'Кнопка "Показать еще" свернулась после возврата на главную');
    });

    it('Популярные категории', async function() {
        const browser = this.browser;
        await browser.yaOpenEcomSpa({
            service: 'ymturbo.t-dir.com',
            pageType: 'main',
        });
        await browser.getText('.Collection .Title')
            .then(text => assert.equal(text, 'Популярные категории', 'Нет блока с названием "Популярные категории"'));
        const collectionItemTitle = await browser.getText('.Collection .Collection-Item:nth-of-type(1) .Collection-ItemInfo p');
        await browser.yaScrollPage('.Collection .Collection-Item:nth-of-type(1)', 0.3);
        await browser.click('.Collection .Collection-Item:nth-of-type(1)');
        await browser.yaWaitForVisible('.EcomScreen');
        await browser.yaWaitForVisible('.ProductList-Content', 'Не загрузился каталог');
        const listTitle = await browser.getText('.EcomScreen_type_product-list .Title');
        assert.strictEqual(collectionItemTitle, listTitle, 'Заголовок страницы не соответствует заголовку категорийной карточки');
    });
});
