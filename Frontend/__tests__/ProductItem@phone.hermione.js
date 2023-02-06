const DISABLE_POPUP = '.BottomBar-ItemPopup { display: none }';

describe('ProductItem', function() {
    it('Внешний вид с товаром под заказ', function() {
        const pathForPatch = 'content.4.content.0.available';
        const valueForPatch = false;

        return this.browser
            .yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: {
                    patch: 'setElement',
                    setElement: `${pathForPatch},${valueForPatch}`,
                },
                style: DISABLE_POPUP,
            })
            .yaWaitForVisible('.ScreenContent')
            .yaMockImages()
            .assertView('plain', '.ProductItem:first-child');
    });

    it('Добавление в корзину с поискового листинга', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'bealab.ru%2Fyandexturbocatalog%2F',
            pageType: 'main',
            query: { patch: 'removeShopLogo' },
            style: DISABLE_POPUP,
        });
        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.click('.SuggestSearch-Form');
        await browser.yaWaitForVisible('.SuggestSearch_focused', 'Не появился фокус в поле поиска');
        await browser.setValue('.SuggestSearch .Textinput-Control', 'гель');
        await browser.click('.SuggestSearch-Button_type_submit');
        await browser.yaWaitForVisible('.EcomScreen_type_product-list .CategoryList', 'Не произошел переход на страницу каталога');
        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaScrollPage('.ProductItem:nth-of-type(1)', 0.3);
        await browser.yaMockImages();
        await browser.assertView('content', ['.ProductItem:nth-of-type(1)', '.ProductItem:nth-of-type(2)']);
        await browser.click('.ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaMockImages();
        await browser.assertView('button', '.ProductItem:nth-of-type(1)');
        await browser.click('.ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaWaitForVisible('.CartHead-Select', 'Не произошел переход в корзину');
        await browser.yaMockImages();
        await browser.assertView('cart', '.CartHead-CartItems');
        await browser.back();
        await browser.yaWaitForVisible('.ProductList.ProductList_type_grid', 'Не произошел возврат на страницу каталога');
    });

    it('Добавление в корзину с главной товара со скидкой', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'bealab.ru%2Fyandexturbocatalog%2F',
            pageType: 'main',
            query: { patch: 'removeShopLogo' },
            style: DISABLE_POPUP,
        });
        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaWaitForVisible('.ProductList_type_horizontal');
        await browser.yaScrollPage('.ProductList_type_horizontal:nth-of-type(6)', 0.3);
        await browser.yaMockImages();
        await browser.assertView('list', '.ProductList_type_horizontal:nth-of-type(6)');
        await browser.yaScrollPage('.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1) .Button2_view_action', 0.3);
        await browser.click('.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaMockImages();
        await browser.assertView('button', '.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1)');
        await browser.click('.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaWaitForVisible('.CartHead-Select', 'Не произошел переход в корзину');
        await browser.yaScrollPage('.CartHead-LoadingArea', 0.3);
        await browser.yaMockImages();
        await browser.assertView('price', '.CartHead-LoadingArea');
        await browser.back();
        await browser.yaWaitForVisible('.ProductList_type_horizontal:nth-of-type(6)', 'Не произошел возврат на страницу каталога');
    });

    it('Добавление в корзину с главной популярного товара', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io%2Fyandexturbocatalog%2F',
            pageType: 'main',
            query: { patch: 'removeShopLogo' },
            style: DISABLE_POPUP,
        });
        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaWaitForVisible('.ProductList_type_horizontal');
        await browser.yaScrollPage('.ProductList_type_horizontal:nth-of-type(6)', 0.3);
        await browser.yaMockImages();
        await browser.assertView('list', '.ProductList_type_horizontal:nth-of-type(6)');
        await browser.yaScrollPage('.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1) .Button2_view_action', 0.3);
        await browser.click('.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaMockImages();
        await browser.assertView('button', '.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1)');
        await browser.click('.ProductList_type_horizontal:nth-of-type(6) .ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaWaitForVisible('.CartHead-Select', 'Не произошел переход в корзину');
        await browser.yaScrollPage('.CartHead-LoadingArea', 0.3);
        await browser.yaMockImages();
        await browser.assertView('cart', '.CartHead-CartItems');
        await browser.back();
        await browser.yaWaitForVisible('.ProductList_type_horizontal:nth-of-type(6)', 'Не произошел возврат на страницу каталога');
    });

    it('Добавление в корзину после перехода в категорию с главной', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'bealab.ru%2Fyandexturbocatalog%2F',
            pageType: 'main',
            query: { patch: 'removeShopLogo' },
            style: DISABLE_POPUP,
        });
        await browser.yaWaitForVisible('.Navigation-Header');
        await browser.yaScrollPage('.CategoryList', 0.3);
        await browser.yaMockImages();
        await browser.assertView('category', '.CategoryList');
        await browser.click('.CategoryList-ItemContainer:nth-child(1) .Link');
        await browser.yaWaitForVisible('.EcomScreen_type_product-list .CategoryList', 'Не произошел переход на страницу каталога');
        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaScrollPage('.ProductItem:nth-of-type(1) .Button2_view_action', 0.3);
        await browser.click('.ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaMockImages();
        await browser.assertView('button', '.ProductItem:nth-of-type(1)');
        await browser.click('.ProductItem:nth-of-type(1) .Button2_view_action');
        await browser.yaWaitForVisible('.CartHead-Select', 'Не произошел переход в корзину');
        await browser.yaScrollPage('.CartHead-LoadingArea', 0.3);
        await browser.yaMockImages();
        await browser.assertView('cart', '.CartHead-CartItems');
        await browser.back();
        await browser.yaWaitForVisible('.ProductItem:nth-of-type(1)', 0.3, 'Не произошел возврат на страницу каталога');
    });

    const isDevelopment = process.env.NODE_ENV === 'development';
    const isTrendboxCIPR = process.env.TRENDBOX_GITHUB_EVENT_TYPE === 'pull_request';
    const isSandboxCIPR = process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.includes('pull');
    if (!isDevelopment && !isTrendboxCIPR && !isSandboxCIPR) {
        hermione.skip.in(/.*/, 'тесты на сторибуке сейчас не работают в castle и релизах');
    }
    describe('storybook', () => {
        it('Внешний вид с длинным названием товара', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('productitem', 'plain');
            await browser.yaChangeKnob('select', 'name', 'Очень_очень_длинное_название_товара_не_влезающее_на_экран_и_обрезается');
            await browser.yaMockImages();
            await browser.assertView('long-word', '.ProductItem-Info');
            await browser.yaChangeKnob('select', 'name', 'Очень очень длинное название товара не влезающее на экран и обрезается');
            // Немного ждём, когда гермиона увидит новый размер элемента.
            await browser.pause(300);
            await browser.yaMockImages();
            await browser.assertView('long-text', '.ProductItem-Info');
        });

        it('Внешний вид в карусели', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('productitem', 'horizontal');
            await browser.yaMockImages();
            await browser.assertView('plain', '.ProductItem');
        });

        it('Внешний вид на выдаче', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('productitem', 'grid');
            await browser.yaMockImages();
            await browser.assertView('plain', '.ProductItem');
        });

        it('Внешний вид с рейтингом в тумбе', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('productitem', 'plain', {
                reviewsPosition: 'thumb',
            });

            await browser.yaMockImages();
            await browser.assertView('plain', '.ProductItem');
        });

        it('Внешний вид с рейтингом в описании', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('productitem', 'plain', {
                reviewsPosition: 'info',
            });

            await browser.yaMockImages();
            await browser.assertView('plain', '.ProductItem');
        });
    });
});
