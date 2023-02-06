import { last } from 'lodash';
import { parseUrl } from 'query-string';
import type { ICounterTree } from '../../../../tests/typings';
import { setSorting, setPriceFilter } from './helpers';

describe('SearchPage', function() {
    hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
    hermione.skip.in('linux-chrome-iphone', 'GOODS-2510: определяется как ПП');
    it('Саджест', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        await bro.yaMockXHR({
            recordData: ['/suggest-market/'],
            urlDataMap: {
                '&part=ipho': [
                    ['tpah', 'iphone', { tpah: [0, 4, 6] }],
                ],
            },
        });
        const input = await bro.$('.Header .mini-suggest__input');

        const value = 'ipho';
        await input.setValue(value);
        await bro.yaWaitForVisible('.mini-suggest__popup .mini-suggest__item', 'саджест не появился');
        await bro.waitUntil(async() => {
            // Кнопки в саджесте инициализируются не сразу,
            // поэтому пробуем нажимать, пока занчение в инпуте не поменяется.
            await bro.click('.mini-suggest__item');
            return value !== await input.getValue();
        }, {
            timeoutMsg: 'саджест не установил новый запрос в поисковую строку',
        });
        await bro.click('.mini-suggest__button');
        await bro.yaWaitForHidden('.mini-suggest__popup', 'саджест не скрылся');
        await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
    });

    hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
    it('Сортировка → Фильтр по цене → Сортировка', async function() {
        const bro = this.browser;

        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=all_filters=0');
        await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');
        await bro.yaWaitForVisible('.ProductListControlsButton_type_price', 'кнопка фильтрации по цене не появилась');

        await setSorting(bro, 'aprice', async() => {
            return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
        });

        await setPriceFilter(bro, { from: '50000' }, async() => {
            return new URL(await bro.getUrl()).searchParams.get('pricefrom') === '50000';
        });
        assert.strictEqual(
            new URL(await bro.getUrl()).searchParams.get('order'),
            'aprice',
            'параметр сортировки не сохранился',
        );

        await setSorting(bro, 'dpop', async() => {
            const searchParams = new URL(await bro.getUrl()).searchParams;
            return searchParams.get('order') === 'dpop';
        });
        assert(!(new URL(await bro.getUrl()).searchParams.get('pricefrom')), 'параметр фильтра по цене не сбросился');
    });

    it('Сортировка → Очистка запроса на крестик', async function() {
        const bro = this.browser;

        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');

        await setSorting(bro, 'aprice', async() => {
            return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
        });

        const before = await bro.getValue('.mini-suggest__input');
        assert.strictEqual(before, 'iphone');
        await bro.click('.mini-suggest__input-clear');
        const after = await bro.getValue('.mini-suggest__input');
        assert.strictEqual(after, '');
    });

    describe('Сортировка', function() {
        it('Применение параметра order', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');

            await setSorting(bro, 'aprice', async() => {
                return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
            }, 'параметр сортировки «сначала недорогие» не установился');
            assert.strictEqual(await bro.getText('.ProductListControlsButton_type_sorting'), 'Сначала недорогие');

            await setSorting(bro, 'dprice', async() => {
                return new URL(await bro.getUrl()).searchParams.get('order') === 'dprice';
            }, 'параметр сортировки «сначала подороже» не установился');
            assert.strictEqual(await bro.getText('.ProductListControlsButton_type_sorting'), 'Сначала подороже');
        });

        it('Применение параметра категорий', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');

            await setSorting(bro, 'aprice', async() => {
                return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
            });

            await bro.yaWaitElementsChanging('.ProductCardsList-Wrapper .ProductCard', {
                timeoutMsg: 'Выдача не обновилась после применения сортировки',
            });
        });
    });

    describe('После СПАС-перехода', function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenSpasUrl('/products/search?text=iphone&promo=nomooa');
            await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
            await bro.yaAssertViewportView('search_page');
        });
    });

    it('Избранное', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=enable_favorites=1');

        await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
        await bro.assertView('plain', '.ProductCardsList-Item');

        await bro.yaShouldBeVisible('.FloatFavoritesButton', 'точка входа в избранное не видна');
    });

    // Формирование запроса за данными браузеронезависимо.
    hermione.only.in(['linux-chrome-iphone']);
    describe('Параметр rs', () => {
        it('Должен сбрасываться при изменении сортировки', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone&rs=foo');
            await bro.yaWaitForSearchPage();

            await setSorting(bro, 'aprice', async() => {
                return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
            }, 'параметр сортировки «сначала недорогие» не установился');

            const xhr = await bro.yaFindXHR(
                ({ spy }) => spy.url.startsWith('/products/api/rr/search') && /[&?]order=aprice/.test(spy.url),
                { timeoutMsg: 'Не был найден запрос за отсортированной выдачей' },
            );

            assert.notProperty(
                parseUrl(xhr.spy.url).query, 'rs',
                'В запросе за отсортированной выдачей присутствует параметр rs',
            );
        });
    });

    hermione.skip.in('appium-chrome-phone', 'не умеет делать throttle()');
    hermione.skip.in('linux-chrome-iphone', 'не умеет делать throttle()');
    it('Сообщение о потери сети', async function() {
        const bro = this.browser;

        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');

        await bro.throttle('offline');

        await bro.click('.ProductListSorting .ProductListDropDown-Select');
        await bro.selectByAttribute('.ProductListSorting .ProductListDropDown-Select', 'value', 'aprice');

        await bro.yaWaitForVisible('.NotificationList', 10000, 'сообщение "Нет интернета" не появилось');
    });

    hermione.skip.in('linux-chrome-iphone', 'GOODS-2510: определяется как ПП');
    it('Флаг tabs_order_version', async function() {
        const bro = this.browser;

        await bro.yaOpenPageByUrl('/products?text=iphone&exp_flags=tabs_order_version=search,images,video,products,all;active_products_tab=1');
        await bro.yaWaitForVisible('.header3__services', 'навигация не появилась');

        await bro.assertView('plain', '.header3__services');
    });

    hermione.also.in('iphone-dark');
    describe('Темная тема', function() {
        it('Внешний вид с настройкой темы "light"', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrlWithColorScheme('/products/search?text=iphone&promo=nomooa&exp_flags=dark_theme_touch=light');
            await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
            await bro.yaAssertViewportView('theme_light');
        });

        it('Внешний вид с настройкой темы "dark"', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrlWithColorScheme('/products/search?text=iphone&promo=nomooa&exp_flags=dark_theme_touch=dark');
            await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
            await bro.yaAssertViewportView('theme_dark');
        });

        it('Внешний вид с настройкой темы "system"', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrlWithColorScheme('/products/search?text=iphone&promo=nomooa&exp_flags=dark_theme_touch=system');
            await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
            await bro.yaAssertViewportView('theme_system');
        });

        hermione.only.in('iphone-dark');
        it('Переключение системной темы под настройкой темы "system"', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrlWithColorScheme('/products/search?text=iphone&promo=nomooa&exp_flags=dark_theme_touch=system', 'light');
            await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
            await bro.yaAssertViewportView('theme_system_before');

            // переключаем media на темную схему без перезагрузки страницы
            const puppeteer = await bro.getPuppeteer();
            const [page] = await puppeteer.pages();
            await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'dark' }]);

            await bro.yaAssertViewportView('theme_system_after');
        });
    });

    it('Сохранение положения скролла', async function() {
        async function getScrollPosition(bro: WebdriverIO.Browser) {
            return bro.execute(() => {
                return document.documentElement.scrollTop;
            });
        }

        const bro = this.browser;
        bro.yaOpenPageByUrl('/products/search?text=iphone');
        const initialScrollPosition = await getScrollPosition(bro);
        assert.equal(
            initialScrollPosition,
            38,
            'Изначально страница должна быть проскроллена к хедеру',
        );

        const scrollValue = 190;

        await bro.yaScroll(scrollValue);

        await bro.click('.ProductCard');
        await bro.yaWaitForVisible('.Card-CloseButton');
        await bro.click('.Card-CloseButton');
        await bro.yaWaitForVisible('.ProductCard');

        assert.equal(
            await getScrollPosition(bro),
            scrollValue,
            'Положение скролла на странице не восстановилось',
        );
    });

    hermione.also.in('linux-searchapp-phone');
    hermione.only.in('linux-searchapp-phone');
    describe('Поисковое приложение', function() {
        it('Скрытие шапки', async function() {
            const bro = this.browser;
            await bro.url('/products/search?text=iphone');
            await bro.yaWaitForVisible('.SearchPage', 'страница не появилась');
            const header = await bro.$('.Header');
            assert.isFalse(await header.isDisplayed(), 'шапка не скрылась');
        });
    });

    // Тесты на счетчики не браузерозависимы, для ускорения гоняем только на одной платформе
    hermione.only.in('linux-chrome-iphone');
    describe('Баобаб', () => {
        it('Отправка события show при переходе на выдачу', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177754');
            await bro.yaWaitForPageLoad();
            await bro.click('.Card-CloseButton');

            await bro.waitUntil(async() => {
                // Ждем когда залогируется событие показа выдачи
                const showEvents = await bro.yaGetBaobabSentEvents('show');
                return Boolean(showEvents.length);
            }, {
                timeout: 5000,
                timeoutMsg: 'событие show не было залогировано',
            });
        });

        it('При возврате на выдачу не должно отправляться событие show', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012');
            await bro.yaWaitForPageLoad();

            await bro.click('.ProductCardsList-Item');
            await bro.yaWaitForHidden('.Spin2_progress', 5000, 'страница товара не загрузилась');
            await bro.click('.Card-CloseButton');

            /** логи проростают не сразу, нужна задержка, чтобы убедится что show не был залогирован */
            await bro.pause(2000);
            const showEvents = await bro.yaGetBaobabSentEvents('show');

            assert.isTrue(showEvents.length === 0, 'Событие show не должно отправляться при возврате на выдачу');
        });

        it('Отправка события show при сортировке выдачи', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012');
            await bro.yaWaitForPageLoad();

            await setSorting(bro, 'aprice', async() => {
                const searchParams = new URL(await bro.getUrl()).searchParams;
                return searchParams.get('order') === 'aprice';
            });

            let showEvent: ICounterTree | undefined;
            await bro.waitUntil(async() => {
                // Ждем когда залогируется новое дерево после сортировки
                showEvent = last(await bro.yaGetBaobabSentEvents('show')) as ICounterTree;
                return Boolean(showEvent);
            }, {
                timeout: 2000,
                timeoutMsg: 'после сортировки выдачи не отправилось событие show',
            });

            const url = showEvent?.tree?.attrs?.url;
            const { searchParams } = new URL(url);

            assert.strictEqual(
                searchParams.get('order'),
                'aprice',
                'в залогированом урле нет параметра сорировки',
            );
        });
    });

    // Тесты на метрики не браузерозависимы, для ускорения гоняем только на двух платформах
    hermione.only.in('linux-chrome-iphone');
    describe('Mertics', async function() {
        it('Запросы', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForPageLoad();

            await bro.click('.ProductCard');
            await bro.yaWaitForHidden('.Spin2_progress', 3000, 'карточка товара не загрузилась');

            await bro.yaCheckMetrics({
                'products.all_requests': 2,
                'products.requests': 1, // Запросы к поисковой выдаче
                'products.sku_requests': 1,
            });

            await bro.click('.Card-CloseButton');

            // Проверяем, что после закрытия модалки не считается еще один запрос
            await bro.yaCheckMetrics({
                'products.all_requests': 2,
                'products.requests': 1,
                'products.sku_requests': 1,
            });
        });
    });

    describe('Быстрые фильтры', async function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=all_filters');
            await bro.yaWaitForPageLoad();

            await bro.assertView('plain', '.Filters-QuickFilters');
        });

        it('Внешний вид примененных фильтров', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=all_filters&glfilter=7893318%3A153043&priceto=50000');
            await bro.yaWaitForPageLoad();

            await bro.assertView('plain', '.Filters-QuickFilters');
        });

        it('Сброс фильтра по крестику', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=all_filters&priceto=50000');
            await bro.yaWaitForPageLoad();

            await bro.click('.Filters-QuickFiltersItem_active .Filters-QuickFiltersItemCross');

            const wait = await bro.yaWaitElementsChanging('.ProductCardsList-Wrapper .ProductCard', {
                timeoutMsg: 'Выдача не обновилась после сброса фильтра',
            });

            await wait();
            const url = new URL(await bro.getUrl());
            assert.notExists(url.searchParams.get('priceto'));
        });

        it('Сброс всех фильтров по крестику', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=all_filters&glfilter=7893318%3A153043&priceto=50000&promo=nomooa');
            await bro.yaWaitForPageLoad();

            await bro.click('.Filters-QuickFiltersCross');

            const wait = await bro.yaWaitElementsChanging('.ProductCardsList-Wrapper .ProductCard', {
                timeoutMsg: 'Выдача не обновилась после сброса фильтров',
            });

            await wait();
            const url = new URL(await bro.getUrl());
            assert.notExists(url.searchParams.get('priceto'));
            assert.notExists(url.searchParams.get('glfilter'));
        });
    });

    describe('Категорийные фильтры', async function() {
        it('Фильтр по цене не сбрасывается при сортировке', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone&priceto=50000&exp_flags=all_filters');
            await bro.yaWaitForPageLoad();

            await setSorting(bro, 'aprice', async() => {
                return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
            });

            const url = new URL(await bro.getUrl());
            assert.exists(url.searchParams.get('priceto'));
        });
    });
});
