const assertPricesLessThenValue = async(browser, value, errorMessage) => {
    const prices = await browser.getText('.Price-Cost_current');
    assert.isTrue(
        prices
            // оставляем в цене только цифры и приводим к number
            .map(price => Number(price.replace(/\D/g, '')))
            .every(price => price <= value),
        errorMessage,
    );
};

describe('Ecom-tap', () => {
    describe('Поиск', () => {
        describe('Страница пустых результатов поиска', () => {
            it('Внешний вид', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                    query: { query: 'qwe' },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.assertView('plain', '.EmptySearchResult');
            });

            it('На странице нет сортировки и фильтров', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                    query: { query: 'qwe' },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                const isFilterExist = await browser.isExisting('.ProductListControl');

                assert.isFalse(isFilterExist, 'На странице есть фильтры');
            });

            it('На странице есть панель фильтров, если есть примененные фильтры', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                    query: {
                        query: 'qwe',
                        filters: 'price:,1',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                const isFilterExist = await browser.isExisting('.ProductListControl');

                assert.isTrue(isFilterExist, 'На странице нет фильтров');
            });
        });

        it('В подкатегориях есть кнопка искать везде', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                expFlags: { 'turbo-app-global-search': 1 },
                query: { query: 'игра' },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            let isSearchAllButtonExist = await browser.isExisting('.SearchCategoryListItem_type_all');
            assert.isFalse(isSearchAllButtonExist, 'Кнопка "искать везде" не должна быть на рутовом каталоге');

            await browser.click('.SearchCategoryListItem');
            await browser.yaWaitForHidden('.Skeleton');
            isSearchAllButtonExist = await browser.isExisting('.SearchCategoryListItem_type_all');

            assert.isTrue(isSearchAllButtonExist, 'Кнопка "искать везде" должна быть на подкаталоге');
        });

        it('Опечаточник пропадает при переходе в подкатегорию', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                expFlags: { 'turbo-app-global-search': 1 },
                query: { query: 'иггра' },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            let isMisspellExist = await browser.isExisting('.Misspell');
            assert.isTrue(isMisspellExist, 'Не появился опечаточник');

            await browser.click('.SearchCategoryListItem');
            await browser.yaWaitForHidden('.Skeleton');
            isMisspellExist = await browser.isExisting('.Misspell');

            assert.isFalse(isMisspellExist, 'Опечатоник остался на странице');
        });

        it('Количество товаров в описании и на кнопке категории совпадают', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                expFlags: { 'turbo-app-global-search': 1 },
                query: { query: 'игра' },
            });

            const categoryBtnSelector = '.SearchCategoryListItem:first-child';

            await browser.yaWaitForVisible('.SearchCategories');
            const countInButton = await browser.getText(`${categoryBtnSelector} .SearchCategoryListItem-Count`);

            await browser.click(categoryBtnSelector);
            await browser.yaWaitForHidden('.Skeleton');

            const disclaimerText = await browser.getText('.SearchCategories-Disclaimer');
            const countInDisclamer = disclaimerText.replace(/\D/g, '');

            assert.equal(countInDisclamer, countInButton, 'Количество товаров не совпадает');
        });

        it('Ссылка из описания категории ведет в категорию без поиска', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                expFlags: { 'turbo-app-global-search': 1 },
                query: {
                    query: 'игра',
                    category_id: 13630,
                    category_count: 3,
                },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            await browser.click('.SearchCategories-Disclaimer .Link');
            await browser.yaWaitForHidden('.Skeleton');

            const url = await browser.getUrl();
            const { searchParams } = new URL(url);

            assert.isNull(searchParams.get('query'), 'Параметр поиска не был удален');
        });

        it('Кнопка искать везде открывает поиск по корневому каталогу', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                expFlags: { 'turbo-app-global-search': 1 },
                query: { query: 'игра' },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            const rootCatalogUrl = new URL(await browser.getUrl());

            // удаляем служебные параметры
            rootCatalogUrl.searchParams.delete('testRunId');
            rootCatalogUrl.searchParams.delete('tpid');
            rootCatalogUrl.searchParams.delete('promo');
            rootCatalogUrl.searchParams.delete('hermione_autoload');
            // сортируем параметры
            rootCatalogUrl.searchParams.sort();

            await browser.click('.SearchCategoryListItem');
            await browser.yaWaitForHidden('.Skeleton');
            await browser.yaWaitForVisible('.SearchCategoryListItem_type_all');
            await browser.click('.SearchCategoryListItem_type_all');
            await browser.yaWaitForVisible('.NavigationTransition_state_entered');

            const currentUrl = new URL(await browser.getUrl());
            // сортируем и фильтруем параметры
            currentUrl.searchParams.delete('testRunId');
            currentUrl.searchParams.delete('tpid');
            currentUrl.searchParams.sort();

            await browser.yaCheckURL(
                rootCatalogUrl.toString(),
                currentUrl.toString(),
                'Не произошел переход на поиск по корневому каталогу',
            );

            const breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
            assert.equal(breadcrumbsText, 'Каталог');
        });

        hermione.only.notIn('iphone', 'В тестовом iphone нестабильно работает подскролл');
        it('Подскролл кнопок-категорий', async function() {
            const browser = this.browser;
            const LIST_SELECTOR = '.CategoryList_orientation_horizontal .CategoryList-List';

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: { 'turbo-app-global-search': 1 },
                query: { query: 'lorem' },
            });

            await browser.yaWaitForVisible('.CategoryList_orientation_horizontal');
            await browser.assertView('categories', '.CategoryList_orientation_horizontal');

            const { value: scrollX } = await browser.execute(() => {
                const listElement = document.querySelector('.CategoryList-List');
                // вычисляем максимально доступный скролл
                return listElement.scrollWidth - listElement.clientWidth;
            });

            await browser.yaScrollElement(LIST_SELECTOR, scrollX);
            // Пробуем стабилизировать тест, чтобы изменение сохранилось, перед уходом со страницы
            await browser.yaWaitUntil('Категории не прокручены до нужного места первый раз', () =>
                browser.execute(function() {
                    return document.querySelector('.CategoryList-List').scrollLeft;
                }).then(({ value }) => value === scrollX)
            );

            await browser.click('.SearchCategoryListItem:last-child');
            await browser.yaWaitForVisible('.ScreenContent');

            const currentUrl = new URL(await browser.getUrl());
            assert.isNotNull(
                currentUrl.searchParams.get('category_id'),
                'Не произошел переход в подкатегорию',
            );

            await browser.back();
            await browser.yaWaitForVisible('.NavigationTransition_state_entered');

            await browser.yaWaitUntil(
                'Категории не прокручены до нужного места',
                () => browser.execute(function() {
                    return document.querySelector('.CategoryList-List').scrollLeft;
                }).then(({ value }) => value === scrollX),
                5000,
                300,
            );
        });

        describe('Навигация', () => {
            it('Возврат назад при поиске из Каталога и Главной', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                    expFlags: { 'turbo-app-global-search': 1 },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.setValue('.Search-Input input', 'lorem');
                // нажатие кнопки enter
                await browser.keys('\uE007');

                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list',
                    null,
                    'Не произошел переход на страницу поиска',
                );

                let breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
                assert.equal(breadcrumbsText, 'Главная');
                await browser.click('.ScreenHeaderBack');

                await browser.yaWaitForVisible(
                    '.EcomScreen_type_main',
                    null,
                    'Не произошел возврат на главную страницу',
                );

                await browser.click('.BottomBar-Item_type_catalog');
                await browser.setValue('.Search-Input input', 'lorem');
                // нажатие кнопки enter
                await browser.keys('\uE007');

                await browser.yaWaitForVisible('.CategoryList_orientation_horizontal');
                breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
                assert.equal(breadcrumbsText, 'Каталог');
                await browser.click('.ScreenHeaderBack');

                await browser.yaWaitForHidden('.SearchCategories');
            });

            it('Возврат назад из карточки товара и категории', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                    query: { query: 'lorem' },
                });

                await browser.yaWaitForVisible('.SearchCategories');
                await browser.click('.SearchCategoryListItem:first-child');

                await browser.yaWaitForVisible('.NavigationTransition_state_entered');
                let currentUrl = new URL(await browser.getUrl());
                assert.isNotNull(
                    currentUrl.searchParams.get('category_id'),
                    'Не произошел переход в подкатегорию',
                );

                let breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
                assert.equal(breadcrumbsText, 'Назад');
                await browser.click('.ScreenHeaderBack');

                await browser.yaWaitForVisible('.NavigationTransition_state_entered');
                currentUrl = new URL(await browser.getUrl());
                assert.isNull(
                    currentUrl.searchParams.get('category_id'),
                    'Не произошел переход в родительскую категорию',
                );

                await browser.click('.ProductItem');
                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product',
                    null,
                    'Не произошел переход на страницу товара',
                );
                await browser.yaWaitForVisible('.ScreenContent');

                breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
                assert.equal(breadcrumbsText, 'Назад');
                await browser.click('.ScreenHeaderBack');

                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list',
                    null,
                    'Не произошел возврат на страницу поиска',
                );
            });

            it('Возврат назад со страницы фильтров', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                    query: { query: 'lorem' },
                });

                await browser.yaWaitForVisible('.SearchCategories');

                await browser.click('.ProductListControl-Option_filter');
                await browser.yaWaitForHidden('.EcomScreen_type_product-list');
                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list-filter',
                    null,
                    'Не произошел переход на страницу фильтров'
                );

                let breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
                assert.equal(breadcrumbsText, 'Каталог');
                await browser.click('.ScreenHeaderBack');

                await browser.yaWaitForVisible('.SearchCategories');
                let categoryName = await browser.getText('.SearchCategoryListItem:first-child');
                // отрезаем количество товаров, оставяем только название категории
                categoryName = categoryName.split('\n')[0].trim();

                await browser.yaWaitForVisible('.NavigationTransition_state_entered');
                await browser.click('.SearchCategoryListItem:first-child');
                await browser.yaWaitForVisible('.NavigationTransition_state_entered');

                await browser.click('.ProductListControl-Option_filter');
                await browser.yaWaitForVisible('.EcomScreen_type_product-list-filter');
                await browser.yaWaitForHidden('.EcomScreen_type_product-list');

                breadcrumbsText = await browser.getText('.ScreenHeaderBack .Button2-Text');
                assert.equal(breadcrumbsText, categoryName);
            });

            it('Возврат назад после применения фильтров', async function() {
                const browser = this.browser;
                function getProductsLength() { return document.querySelectorAll('.ProductItem').length }

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                    query: {
                        query: 'esse',
                        category_id: '9',
                        category_count: '2',
                    },
                });

                await browser.yaWaitForVisible('.SearchCategories');
                const { value: productsCountBeforeFilter } = await browser.execute(getProductsLength);

                await browser.click('.ProductListControl-Option_filter');
                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list-filter',
                    null,
                    'Не произошел переход на страницу фильтров'
                );

                await browser.pause(2000);
                await browser.setValue('input[name="to"]', '1400');
                await browser.click('.EcomListFilter-Action .Button');

                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list',
                    null,
                    'Не произошел переход на страницу поиска после применения фильтров'
                );

                const { value: productsCountAfterFilter } = await browser.execute(getProductsLength);

                assert.isTrue(
                    productsCountBeforeFilter > productsCountAfterFilter,
                    'Количество товаров после применения фильтров не изменилось',
                );

                await browser.click('.ScreenHeaderBack');
                await browser.yaWaitForVisible('.NavigationTransition_state_entered');

                const { value } = await browser.execute(getProductsLength);
                assert.equal(
                    productsCountBeforeFilter,
                    value,
                    'Фильтры не сбросились при переходе назад по истории',
                );
            });

            it('Проброс фильтров в подкатегорию и обратно', async function() {
                const browser = this.browser;
                const PRICE_UP_TO = 2600;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    expFlags: { 'turbo-app-global-search': 1 },
                });

                await browser.yaWaitForVisible('.ScreenContent');

                await browser.yaScrollPage('.ProductListControl-Option_filter', 0.3);
                await browser.click('.ProductListControl-Option_filter');
                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list-filter',
                    null,
                    'Не произошел переход на страницу фильтров'
                );

                await browser.pause(2000);
                await browser.setValue('input[name="to"]', PRICE_UP_TO);
                await browser.click('.EcomListFilter-Action .Button');

                await browser.yaWaitForVisible(
                    '.EcomScreen_type_product-list',
                    null,
                    'Не произошел переход на страницу поиска после применения фильтров'
                );

                await browser.yaWaitForHidden('.Skeleton');
                await assertPricesLessThenValue(
                    browser,
                    PRICE_UP_TO,
                    'В списке есть товары не соответствующие примененным фильтрам',
                );

                await browser.click('.CategoryList-Item');
                await browser.yaWaitForHidden('.Skeleton');
                await assertPricesLessThenValue(
                    browser,
                    PRICE_UP_TO,
                    'В списке есть товары не соответствующие примененным фильтрам',
                );

                await browser.click('.ScreenHeaderBack');
                await browser.yaWaitForVisible('.NavigationTransition_state_entered');
                await assertPricesLessThenValue(
                    browser,
                    PRICE_UP_TO,
                    'В списке есть товары не соответствующие примененным фильтрам',
                );
            });
        });
    });
});
