import { last } from 'lodash';
import { parseUrl } from 'query-string';
import type { ICounterTree } from '../../../../tests/typings';
import { setSorting, setPriceFilter } from './helpers';

describe('SearchPage', function() {
    it('Саджест', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        await bro.yaMockXHR({
            recordData: ['/suggest-market/'],
            urlDataMap: {
                '&part=ipho': [
                    ['fulltext', 'iphone', {}],
                ],
            },
        });

        const value = 'ipho';
        await bro.setValue('.Header .mini-suggest__input', value);
        await bro.yaWaitForVisible('.mini-suggest__popup .mini-suggest__item', 'саджест не появился');
        await bro.click('.mini-suggest__item');
        await bro.yaWaitForHidden('.mini-suggest__popup', 'саджест не скрылся');
        await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');
        const suggestedValue = await bro.getValue('.Header .mini-suggest__input');
        assert.isTrue(value !== suggestedValue, 'саджест не установил новый запрос в поисковую строку');
    });

    it('Сортировка → Фильтр по цене → Сортировка', async function() {
        const bro = this.browser;

        if (await bro.getMeta('platform') === 'desktop') {
            // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
            await bro.setWindowSize(1280, 600);
        }

        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=all_filters_desktop=0');
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

    describe('Сортировка', function() {
        it('Применение параметра order', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

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

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');

            await setSorting(bro, 'aprice', async() => {
                return new URL(await bro.getUrl()).searchParams.get('order') === 'aprice';
            }, 'параметр сортировки «сначала недорогие» не установился');

            await bro.yaWaitElementsChanging('.ProductCardsList-Wrapper .ProductCard', {
                timeoutMsg: 'Выдача не обновилась после применения сортировки',
            });
        });
    });

    // Формирование запроса за данными браузеронезависимо.
    hermione.only.in(['linux-chrome']);
    describe('Параметр rs', () => {
        it('Должен сбрасываться при изменении сортировки', async function() {
            const bro = this.browser;

            // Уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
            await bro.setWindowSize(1280, 600);

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

    hermione.skip.in('linux-chrome', 'не умеет делать throttle()');
    hermione.skip.in('linux-firefox', 'не умеет делать throttle()');
    it('Сообщение о потери сети', async function() {
        const bro = this.browser;

        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        await bro.yaWaitForVisible('.ProductListControlsButton_type_sorting', 'кнопка сортировки не появилась');

        await bro.throttle('offline');

        await bro.click('.ProductListControlsButton_type_sorting');
        await bro.yaWaitForVisible('.ProductListSorting .ProductListDropDown-Popup', 'список сортировки не появился');

        await bro.click('.ProductListSorting .ProductListDropDown-PopupButton[value="aprice"]');

        await bro.yaWaitForVisible('.NotificationList', 10000, 'сообщение "Нет интернета" не появилось');
    });

    it('Флаг tabs_order_version', async function() {
        const bro = this.browser;

        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=tabs_order_version=search,images,video,products,all;active_products_tab=1');
        await bro.yaWaitForVisible('.navigation', 'навигация не появилась');

        await bro.assertView('plain', '.navigation');
    });

    // Тесты на счетчики не браузерозависимы, для ускорения гоняем только на одной платформе
    hermione.only.in('linux-chrome');
    describe('Баобаб', () => {
        it('Открытие товара в модалке с выдачи', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012');
            await bro.yaWaitForPageLoad();
            await bro.click('.ProductCardsList-Item');

            await bro.yaWaitForHidden('.Spin2_progress', 5000, 'карточка товара в модалке не загрузилась');

            await bro.waitUntil(async() => {
                // Ждем когда залогируется событие показа модалки
                const showEvent = last(await bro.yaGetBaobabSentEvents('show'));
                return Boolean(showEvent);
            }, {
                timeout: 3000,
                timeoutMsg: 'не залогировалось событие показа модалки с товаром',
            });
        });

        it('Отправка события show при переходе на выдачу', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177754');
            await bro.yaWaitForPageLoad();
            await bro.click('.ProductEntityModal-CloseButton');

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
            await bro.yaWaitForHidden('.Spin2_progress', 5000, 'карточка товара в модалке не загрузилась');
            await bro.click('.ProductEntityModal-CloseButton');

            /** логи проростают не сразу, нужна задержка, чтобы убедится что show не был залогирован */
            await bro.pause(2000);
            const showEvents = await bro.yaGetBaobabSentEvents('show');

            assert.isTrue(showEvents.length === 0, 'Событие show не должно отправляться при возврате на выдачу');
        });

        it('Отправка события show при сортировке выдачи', async function() {
            const bro = this.browser;

            // Уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
            await bro.setWindowSize(1280, 600);

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
    hermione.only.in(['linux-chrome']);
    describe('Mertics', async function() {
        it('Запросы', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForPageLoad();

            await bro.yaCheckMetrics({
                'products.all_requests': 1,
                'products.requests': 1, // Запросы к поисковой выдаче
            });

            await bro.click('.ProductCard');
            await bro.yaWaitForHidden('.Spin2_progress', 3000, 'карточка товара не загрузилась');

            await bro.yaCheckMetrics({
                'products.all_requests': 2,
                'products.requests': 1,
                'products.sku_requests': 1,
            });

            await bro.click('.ProductEntityModal-CloseButton');

            // Проверяем, что после закрытия модалки не считается еще один запрос
            await bro.yaCheckMetrics({
                'products.all_requests': 2,
                'products.requests': 1,
                'products.sku_requests': 1,
            });
        });
    });
});
