async function get(method, ...args) {
    const res = await method.apply(this, args);

    // Иногда предыдущий экран не успевает скрыться,
    // но следующий экран уже появился и гермиона его заметила,
    // в этом случае в DOM существует одновременно две кнопки «Назад»,
    // а нас интересует только кнопка со следующего экрана.
    if (Array.isArray(res)) {
        return res[res.length - 1];
    }

    return res;
}

async function getText(selector) {
    return get.call(this, this.browser.getText, selector);
}

async function getTag(selector) {
    return get.call(this, this.browser.getTagName, selector);
}

async function getAttr(selector, attrName) {
    return get.call(this, this.browser.getAttribute, selector, attrName);
}

async function assertBack(expectedText) {
    let backText1 = '';

    await this.browser.yaWaitForVisible('.ScreenHeaderBack', 'кнопка возврата не появилась');

    return this.browser.yaWaitUntil(() => {
        return `Ожидается текст кнопки возврата «${expectedText}» вместо «${backText1}».`;
    }, async() => {
        backText1 = await getText.call(this, '.ScreenHeaderBack');
        return backText1 === expectedText;
    }, 2000, 100);
}

async function CategorySubcategoryProduct(expectedBackText1) {
    const browser = this.browser;

    // → Категория
    // На главной категории обрезаются кнопкой «Ещё N разделов», но в каталоге нет.
    const isMoreVisible = await browser.isVisible('.CategoryList-ItemMore');
    if (isMoreVisible) {
        await browser.click('.CategoryList-ItemMore');
    }
    // Находим категорию «Обувь», у неё есть подкатегории.
    await browser.click('.CategoryList-Item[href*="category_id=1&"]');
    await browser.yaWaitForVisible('.EcomScreen_type_product-list', 'экран со списком товаров не появился');
    await assertBack.call(this, expectedBackText1);
    const backTag1 = await getTag.call(this, '.ScreenHeaderBack');
    assert.strictEqual(backTag1, 'button');
    const title1 = await getText.call(this, '.Title');

    // → Подкатегория
    // Иногда как-будто обработчик не успевает установиться до клика.
    await browser.pause(500);
    await browser.click('.CategoryList-Item');
    let title2 = await getText.call(this, '.Title');
    await browser.yaWaitUntil('Подкатегория не открылась', async() => {
        title2 = await getText.call(this, '.Title');
        return title1 !== title2;
    }, 2000, 100);
    await assertBack.call(this, title1);
    const backTag2 = await getTag.call(this, '.ScreenHeaderBack');
    assert.strictEqual(backTag2, 'button');

    // → Товар
    await browser.yaWaitForVisible('.ProductItem', 'товар не появился');
    await browser.click('.ProductItem .Link');
    await browser.yaWaitForVisible('.EcomScreen_type_product', 'экран с товаром не появился');
    await assertBack.call(this, title2);
    const backTag3 = await getTag.call(this, '.ScreenHeaderBack');
    assert.strictEqual(backTag3, 'button');
}

describe('Navigation', () => {
    describe('Кнопка назад', () => {
        it('Главная → Категория → Подкатегория → Товар', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
            });

            // Главная
            await browser.yaWaitForVisible('.EcomScreen_type_main', 'главный экран не появился');
            await browser.yaShouldNotBeVisible('.ScreenHeaderBack', 'На главной видна кнопка назад');

            await CategorySubcategoryProduct.call(this, 'Главная');
        });

        it('Главная → Товар → Быстрый заказ', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
            });

            // Товар
            await browser.click('.ProductItem .Link');
            await browser.yaWaitForVisible('.EcomScreen_type_product', 'экран с товаром не появился');
            await assertBack.call(this, 'Главная');
            const backTag1 = await getTag.call(this, '.ScreenHeaderBack');
            assert.strictEqual(backTag1, 'button');

            // → Быстрый заказ
            // Подскрол со сдвигом, чтобы кнопка не скрывалась под навигационной панелью.
            await browser.yaScrollPage('.ProductScreen-Actions-Button_oneClick', 0.3);
            // Небольшая задержка, потому что searchapp не успевает нормально кликнуть на кнопку после скрола.
            await browser.pause(300);
            await browser.click('.ProductScreen-Actions-Button_oneClick');
            await browser.yaWaitForVisible('.EcomCartForm', 'окно быстрого заказа не появилось');
            await assertBack.call(this, 'Назад');
            const backTag2 = await getTag.call(this, '.ScreenHeaderBack');
            assert.strictEqual(backTag2, 'button');
        });

        it('Каталог → Категория → Подкатегория → Товар', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
            });

            // Каталог
            await browser.yaWaitForVisible('.EcomScreen_type_product-list', 'экран со списком товаров не появился');
            await browser.yaShouldNotBeVisible('.ScreenHeaderBack', 'В каталоге видна кнопка назад');

            await CategorySubcategoryProduct.call(this, 'Каталог');
        });

        it('Каталог → Товар', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
            });

            // → Товар
            await browser.click('.ProductItem .Link');
            await browser.yaWaitForVisible('.EcomScreen_type_product', 'экран с товаром не появился');
            await assertBack.call(this, 'Каталог');
            const backTag1 = await getTag.call(this, '.ScreenHeaderBack');
            assert.strictEqual(backTag1, 'button');
        });

        it('Товар', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    pcgi: 'rnd%3D2lum7hf3',
                    product_id: '202',
                },
            });

            await browser.yaWaitForVisible('.EcomScreen_type_product', 'экран с товаром не появился');
            await assertBack.call(this, 'Спортивная Одежда');
            const backTag1 = await getTag.call(this, '.ScreenHeaderBack');
            assert.strictEqual(backTag1, 'a');
            const backHref = await getAttr.call(this, '.ScreenHeaderBack', 'href');
            assert.include(backHref, '/turbo/spideradio.github.io/n/yandexturbocatalog/');
        });

        it('Товар → Главная → Категория → Товар ← Категория', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    pcgi: 'rnd%3D2lum7hf3',
                    product_id: '202',
                },
            });

            // → Главная
            await browser.click('.BottomBar-Item:nth-child(1)');

            // → Категория
            await browser.yaWaitForVisible('.CategoryList', 'категории не появились');
            await browser.click('.CategoryList-Item');
            await assertBack.call(this, 'Главная');
            const title1 = await getText.call(this, '.Title');

            // → Товар
            await browser.yaWaitForVisible('.ProductItem', 'товар не появился');
            await browser.click('.ProductItem .Link');
            await assertBack.call(this, title1);

            // ← Категория
            // iphone часто не успевает повесить обработчик на нажатие по кнопке возврата
            await browser.pause(300);
            await browser.click('.ScreenHeaderBack');
            await assertBack.call(this, 'Главная');
        });

        it('Главная → Корзина → Товар', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                expFlags: { turboforms_endpoint: '/empty/' },
            });

            // → Корзина
            await browser.yaWaitForVisible('.ProductList', 'список товаров не появился');
            // Подскрол со сдвигом, чтобы кнопка не скрывалась под шапкой.
            await browser.yaScrollPage('.ProductList .ProductItem:nth-child(1) .ProductItem-Action', 0.3);
            await browser.click('.ProductList .ProductItem:nth-child(1) .ProductItem-Action');
            await browser.yaWaitForVisible('.ProductList .ProductItem:nth-child(1) .ProductItem-Action_inCart', 'товар не положился в корзину');
            await browser.click('.ProductList .ProductItem:nth-child(1) .ProductItem-Action_inCart');

            // → Товар
            await browser.yaWaitForVisible('.ProductItem', 'ссылка на товар не появилась');
            // iphone часто не успевает повесить обработчик на нажатие по кнопке возврата
            await browser.pause(600);
            await browser.click('.ProductItem .Link');
            await assertBack.call(this, 'Корзина');
        });

        it('Фасетная категория → Подкатегория → Каталог', async function() {
            const browser = this.browser;

            // https://yandex.ru/turbo/mnogodivanov.ru/n/yandexturbolisting/katalog/kozhanie-divani
            await browser.yaOpenEcomSpa({
                url: '/turbo/mnogodivanov.ru/n/yandexturbolisting/katalog/kozhanie-divani',
            });

            await assertBack.call(this, 'Главная');
            const title1 = await getText.call(this, '.Title');
            assert.strictEqual(title1, 'Кожаные диваны');

            // → Подкатегория
            await browser.click('.CategoryList-Item');
            let title2 = await getText.call(this, '.Title');
            await browser.yaWaitUntil('Подкатегория не открылась', async() => {
                title2 = await getText.call(this, '.Title');
                return title1 !== title2;
            }, 2000, 100);

            // → Каталог
            await browser.click('.BottomBar-Item:nth-child(2)');
            await browser.yaShouldNotBeVisible('.ScreenHeaderBack', 'В каталоге видна кнопка назад');
            await browser.yaShouldNotBeVisible('.Title', 'В каталоге виден заголовок');
        });

        it('Фасетная категория → Подкатегория → Родителькая категория', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { category_id: 1 },
            });

            await assertBack.call(this, 'Каталог');
            const title1 = await getText.call(this, '.Title');
            assert.strictEqual(title1, 'Обувь');

            // → Подкатегория
            await browser.click('.CategoryList-Item');
            let title2 = await getText.call(this, '.Title');
            await browser.yaWaitUntil('Подкатегория не открылась', async() => {
                title2 = await getText.call(this, '.Title');
                return title1 !== title2;
            }, 2000, 100);
            await assertBack.call(this, title1);

            await browser.refresh();

            await browser.yaWaitForVisible('.ScreenHeaderBack');
            await assertBack.call(this, title1);

            // → Родительская категория
            await browser.click('.ScreenHeaderBack');
            await browser.yaWaitUntil('Родительская категория не открылась', async() => {
                const currentTitle = await getText.call(this, '.Title');
                return title1 === currentTitle;
            }, 2000, 100);
        });

        it('Каталог → Поиск → Пустая страница → Переход на страницу товара', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: {
                    query: 'empty123456',
                }
            });

            await browser.yaWaitForVisible('.ProductListScreen-MainBlocks .ProductList');
            await browser.yaWaitForVisible('.GlobalProductList');
            await browser.click('.ProductItem');
            await browser.yaWaitForVisible('.NavigationTransition_state_entered');
            await assertBack.call(this, 'Назад');
            // → Назад
            await browser.click('.ScreenHeaderBack');
            await browser.yaShouldBeVisible('.EmptySearchResult');
        });

        it('Навигация после сортировки товаров', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
            });

            await browser.yaWaitForVisible('.ScreenContent');
            await browser.yaScrollPage('.ProductListControl-Option', 0.3);
            await browser.selectByValue('.ProductListControl-Option select', 'sales');

            await browser.yaWaitForVisible('.ProductItem');
            await browser.yaScrollPage('.ProductItem', 0.3);
            await browser.click('.ProductItem');
            await browser.yaWaitForVisible('.NavigationTransition_state_entered');

            await assertBack.call(this, 'Каталог');
            const backTag1 = await getTag.call(this, '.ScreenHeaderBack');
            assert.strictEqual(backTag1, 'button');
        });
    });
});
