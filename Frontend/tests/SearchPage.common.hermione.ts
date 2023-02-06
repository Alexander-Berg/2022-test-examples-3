import { last } from 'lodash';
import { parseUrl } from 'query-string';
import type { IEventData } from '@yandex-int/react-baobab-logger/lib/common/Baobab/Baobab.typings/Baobab_dynamic';
import type { ISessionData } from '@src/utils/metrika/initEcomSessionLogger';
import { setPriceFilter } from './helpers';
import type { ICounterTree } from '../../../../tests/typings';

describe('SearchPage', function() {
    it('Поиск, выдача и отсутствие оффера', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone&i-m-a-hacker=1&promo=nomooa&offer_does_not_exist=1');

        await bro.yaWaitForVisible('.NotFound', 'сообщение об отсутствии товара не появилось');

        await bro.yaAssertViewportView('plain');
    });

    it('Пустая выдача и отсутствие оффера', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?offer_does_not_exist=1');

        await bro.yaWaitForVisible('.NotFound', 'сообщение об отсутствии товара не появилось');
    });

    it('Очистка поисковой строки', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        const input = await bro.$('.Header .mini-suggest__input');
        const clearButton = await bro.$('.Header .mini-suggest__input-clear');

        assert.strictEqual(await input.getValue(), 'iphone', 'поисковая строка пустая');

        await clearButton.click();
        assert.strictEqual(await input.getValue(), '', 'поисковая строка не пустая');
    });

    it('Редирект', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products?text=iphone');
        await bro.yaWaitForVisible('.SearchPage-Products', 'выдача не появилась');

        let { pathname } = new URL(await bro.getUrl());
        assert.strictEqual(pathname, '/products/search', 'не произошло редиректа на страницу поисковой выдачи');
    });

    it('Опечатка', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=кампьютер');
        await bro.yaWaitForVisible('.Misspell', 'опечаточник не появился');
        const input = await bro.$('.Header .mini-suggest__input');

        assert.strictEqual(await input.getValue(), 'компьютер', 'в поисковой строке не исправился запрос');
        assert.include(await bro.getText('.Misspell'), 'кампьютер', 'опечаточник не содержит исходные запрос');

        await bro.click('.Misspell-Cancel');
        await bro.yaWaitForHidden('.Misspell', 'опечаточник не скрылся');
        assert.strictEqual(await input.getValue(), 'кампьютер', 'в поисковой строке не появился исходный запрос');
        const noreask = new URL(await bro.getUrl()).searchParams.get('noreask');
        assert.strictEqual(noreask, '1', 'параметр отмены исправления опечатки не установился');
    });

    it('Дозагрузка', async function() {
        const bro = this.browser;

        if (await bro.getMeta('platform') === 'desktop') {
            // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
            await bro.setWindowSize(1280, 600);
        }

        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=more_button_items_count=24');
        await bro.yaWaitForVisible('.SearchPage-Products');

        let { length: curItemsLength } = await bro.$$('.ProductCardsList-Wrapper .ProductCardsList-Item');
        assert.isAtLeast(curItemsLength, 12, 'не хватает товаров на выдаче');

        await bro.scroll(0, 20000);
        await bro.yaWaitForHidden('.ProductCardsList-Skeletons', 5000, 'не загрузилась следующая страница');

        await bro.waitUntil(async() => {
            // Смотрим количество элементов имеено во Wrapper, чтобы не учитывать элементы в Skeletons.
            const { length } = await bro.$$('.ProductCardsList-Wrapper .ProductCardsList-Item');
            if (length > curItemsLength) {
                curItemsLength = length;
                return true;
            }
            return false;
        }, {
            timeout: 5000,
            timeoutMsg: 'не загрузились товары со следующей страницы по доскроллу',
        });

        await bro.waitForExist('.SearchPage-MoreButton');

        await bro.click('.SearchPage-MoreButton');
        await bro.waitUntil(async() => {
            // Смотрим количество элементов имеено во Wrapper, чтобы не учитывать элементы в Skeletons.
            const { length } = await bro.$$('.ProductCardsList-Wrapper .ProductCardsList-Item');
            return length > curItemsLength;
        }, {
            timeout: 5000,
            timeoutMsg: 'не загрузились товары со следующей страницы по клику',
        });
    });

    it('Пустая выдача', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=0&promo=nomooa');

        await bro.yaWaitForVisible('.SearchPage');

        await bro.yaAssertViewportView('plain');
    });

    describe('Фильтр по б/у', () => {
        it('Присутствует в списке быстрых фильтров на третьей позиции', async function() {
            const bro = this.browser;

            const isDesktop = await bro.getMeta('platform') === 'desktop';

            if (isDesktop) {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=used_goods=1');
            await bro.yaWaitForVisible('.Filters-QuickFilters');
            await bro.$('.Filters-QuickFilters').scrollIntoView({ block: 'center' });

            const filterItemSelector = isDesktop ? '.QuickFiltersItem' : '.Filters-QuickFiltersItem';

            await bro.assertView('used-goods-quick-filter', `${filterItemSelector}:nth-child(3)`);
        });
    });

    // Формирование запроса за данными браузеронезависимо.
    hermione.only.in(['linux-chrome', 'linux-chrome-iphone']);
    describe('Параметр rs', () => {
        it('Должен присутствовать при дозагрузке', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone&rs=foo');
            await bro.yaWaitForSearchPage();

            await bro.scroll(0, 20000);
            await bro.yaWaitForSearchPage({ waitMore: true });

            const xhr = await bro.yaFindXHR(({ spy }) => spy.url.startsWith('/products/api/rr/search'), {
                timeoutMsg: 'Не был найден запрос за следующей страницей',
            });

            assert.propertyVal(
                parseUrl(xhr.spy.url).query, 'rs', 'foo',
                'В запросе за следующей страницей отсутствует параметр rs или не соответствует его значение',
            );
        });
    });

    describe('Фильтр по цене', () => {
        it('Внешний вид', async function() {
            const bro = this.browser;
            const isDesktop = await bro.getMeta('platform') === 'desktop';

            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=GOODS_enable_price_filter&exp_flags=all_filters=0;all_filters_desktop=0');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');
            await bro.click('.ProductListControlsButton_type_price');

            const priceFilterElementSelector = isDesktop ? '.ProductListPriceFilter-Popup' : '.ProductListPriceFilter-Drawer';

            await bro.yaWaitForVisible(priceFilterElementSelector);

            await bro.yaWaitForVisible('.Slider');

            await bro.assertView('plain', priceFilterElementSelector);
        });

        hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
        it('Цена от', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=GOODS_enable_price_filter&exp_flags=all_filters=0;all_filters_desktop=0');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');
            await setPriceFilter(bro, { from: '50000' });
            await bro.assertView('button', '.ProductListControlsButton_type_price');
        });

        hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
        it('Цена до', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=GOODS_enable_price_filter&exp_flags=all_filters=0;all_filters_desktop=0');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');
            await setPriceFilter(bro, { to: '20000' });
            await bro.assertView('button', '.ProductListControlsButton_type_price');
        });

        hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
        hermione.skip.in('linux-chrome-iphone', 'Расскипать в GOODS-789');
        it('Обе цены', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=GOODS_enable_price_filter&exp_flags=all_filters=0;all_filters_desktop=0');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');
            await setPriceFilter(bro, { from: '10000', to: '20000' });
            await bro.assertView('button', '.ProductListControlsButton_type_price');
        });

        hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
        it('Без товаров', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone&pricefrom=7000000&exp_flags=all_filters=0;all_filters_desktop=0');
            await bro.yaWaitForVisible('.SearchPage-ErrorReport', 2000, 'сообщение о пустой выдаче не появилось');

            await bro.yaShouldBeVisible('.ProductListControlsButton_type_price', 'фильтр по цене отсутствует на странице');
        });
    });

    hermione.skip.in('linux-chrome', 'Расскипать в GOODS-1294');
    describe('Клик по табу в шапке', () => {
        it('Сохранение запроса и фильтров', async function() {
            const bro = this.browser;
            const selector = await bro.getMeta('platform') === 'desktop' ? '.service_name_products .link' : '.header3__service_current';

            await bro.yaOpenPageByUrl('/products/search?order=dpop&priceto=645212&text=iphone%2012');
            await bro.yaWaitForVisible(selector, 'Табы не отобразились');

            await bro.click(selector);
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'iphone 12', 'не сохранился запрос после клика по табу');
            assert.strictEqual(searchParams.get('priceto'), '645212', 'не сохранился фильтр после клика по табу');
        });
    });

    // тесты на счетчики не браузерозависимы, для ускорения гоняем только на двух платформах
    hermione.only.in(['linux-chrome', 'linux-chrome-iphone']);
    describe('Баобаб', () => {
        it('Отправка дерева в blockstat', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012');
            await bro.yaWaitForPageLoad();

            const reqid = await bro.yaGetReqId();

            const { client } = await bro.getCounters(reqid);
            const serverTree = await bro.yaGetBaobabTree();
            const subservice = serverTree?.tree?.attrs?.subservice;

            assert.strictEqual(serverTree.event, 'show', 'дерево с событием show не отправилось в blockstat');
            assert.strictEqual(subservice, 'search', 'не правильный subservice в атрибутах дерева');
            assert.isEmpty(client, 'при загрузке страницы логи не должны отправляться с клиента');
        });

        it('Соответствие деревьев на клиенте и на сервере', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012&exp_flags=validate_counters');
            await bro.yaWaitForPageLoad();

            await bro.yaCompareServerAndClientBaobabTree();
        });

        it('Логирование дозагруки выдачи', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012');
            await bro.yaWaitForPageLoad();

            await bro.scroll(0, 20000);
            await bro.yaWaitForHidden('.ProductCardSkeleton', 5000, 'не загрузилась следующая страница');

            let lastAppend: IEventData | undefined;
            await bro.waitUntil(async() => {
                // Ждем когда залогируется событие дозагрузки
                const appendEvents = await bro.yaGetBaobabSentEvents('append');

                if (appendEvents.length) {
                    lastAppend = appendEvents[0];
                    return true;
                }
                return false;
            }, {
                timeout: 5000,
                timeoutMsg: 'не залогировалась загрузка следующей страницы',
            });

            const pageNode = await bro.yaGetBaobabNode({ path: '$page.$main' });

            assert.strictEqual(
                pageNode?.id,
                lastAppend?.tree?.attrs?.['parent-id'],
                'событие дозагрузки append не записалось в $main',
            );
        });

        it('Отправка события show при фильтрации выдачи по цене', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012&exp_flags=all_filters=0;all_filters_desktop=0');
            await bro.yaWaitForPageLoad();
            await setPriceFilter(bro, { from: '90000' });
            await bro.yaWaitForHidden('.ProductCardsList-Shadow', 5000);

            let showEvent: ICounterTree | undefined;
            await bro.waitUntil(async() => {
                // Ждем когда залогируется новое дерево после фильтрации
                showEvent = last(await bro.yaGetBaobabSentEvents('show')) as ICounterTree;

                return Boolean(showEvent);
            }, {
                timeout: 2000,
                timeoutMsg: 'после фильтрации выдачи по цене не отправилось событие show',
            });

            const url = showEvent?.tree?.attrs?.url;
            const { searchParams } = new URL(url);

            assert.strictEqual(
                searchParams.get('pricefrom'),
                '90000',
                'в залогированом урле нет параметра фильтрации по цене',
            );
        });

        it('Отправка parent-reqid в url-атрибуте $page', async function() {
            const bro = this.browser;

            if (await bro.getMeta('platform') === 'desktop') {
                // На десктопах уменьшаем высоту окна, чтобы сразу не загружалась следующая страница товаров.
                await bro.setWindowSize(1280, 600);
            }

            await bro.yaOpenPageByUrl('/products/search?text=iphone%2012&parent-reqid=123');
            await bro.yaWaitForPageLoad();

            const serverTree = await bro.yaGetBaobabTree();
            const loggedUrl = new URL(serverTree.tree.attrs?.url);
            assert.equal(loggedUrl.searchParams.get('parent-reqid'), '123', 'неверный parent-reqid в url-атрибуте $page');

            const url = await bro.getUrl();
            const { searchParams } = new URL(url);
            assert(!searchParams.has('parent-reqid'), 'parent-reqid не удалился из url страницы');
        });
    });

    describe('Залогин в шапке', () => {
        it('После открытия и закрытия оффера', async function() {
            const bro = this.browser;

            await bro.authOnRecord('plain-user');

            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');

            await bro.click('.ProductCardsList-Item:first-child');

            const closeButtonSelector = await bro.getMeta('platform') === 'desktop' ? '.ProductEntityModal-CloseButton' : '.Card-CloseButton';
            await bro.yaWaitForVisible(closeButtonSelector);
            await bro.click(closeButtonSelector);

            await bro.yaWaitForVisible('.UserID-Wrapper');
            await bro.click('.UserID-Badge');
            await bro.yaWaitForVisible('.UserWidget-Iframe', 'Попап залогина не открылся');
        });
    });

    // тесты на счетчики не браузерозависимы, для ускорения гоняем только на двух платформах
    hermione.only.in(['linux-chrome', 'linux-chrome-iphone']);
    describe('Цели метрики', () => {
        it('По переходу на оффер', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=олег+газманов');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');

            await bro.click('.ProductCard_type_offer');

            await bro.yaCheckMetrikaGoal({
                counterId: '84153601',
                name: 'from-search-to-offer',
                params: {
                    offerId: '7kKU5DMvw8Of8ce5cBlDEQ',
                },
            });
        });

        it('По переходу на sku', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');

            await bro.click('.ProductCard_type_sku');

            await bro.yaCheckMetrikaGoal({
                counterId: '84153601',
                name: 'from-search-to-sku',
                params: {
                    skuId: '101446185733',
                },
            });
        });

        describe('Старт сессии', async function() {
            const goalParams = {
                counterId: '84153601',
                name: 'ecom-session-start',
            };

            it('Отправляется при первом заходе', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/search?text=iphone');
                await bro.yaCheckMetrikaGoal(goalParams);

                const goals = await bro.yaGetMetrikaGoals(goalParams);
                assert.equal(goals.length, 1, `Должна быть отправлена только одна цель "${goalParams.name}"`);
            });

            it('Не отправляется при повторном заходе в течение таймаута одной сессии', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/search?text=iphone');

                const now = await bro.execute(function() {
                    return window.Date.now();
                });

                /** переводим время в сохраненном в localStorage значении на 25 мин назад */
                const savedLsSessionData: ISessionData = {
                    lastTick: now - 25 * 60 * 1000,
                };
                await bro.setLocalStorage('PRODUCTS_ecomSession', JSON.stringify(savedLsSessionData));

                await bro.refresh();

                /** проверяем, что не отправится цель о старте сессии */
                let noGoalsSent = true;
                try {
                    await bro.waitUntil(
                        async() => {
                            const goals = await bro.yaGetMetrikaGoals(goalParams);
                            noGoalsSent = goals.length === 0;
                            return !noGoalsSent;
                        },
                        { timeout: 3000 },
                    );
                } catch {}
                assert.isOk(noGoalsSent, `Не должно быть отправлено ни одной цели "${goalParams.name}"`);
            });

            it('Отправляется при повторном заходе после таймаута одной сессии', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/search?text=iphone');

                const now = await bro.execute(function() {
                    return window.Date.now();
                });

                /** переводим время в сохраненном в localStorage значении на 35 мин назад */
                const savedLsSessionData: ISessionData = {
                    lastTick: now - 35 * 60 * 1000,
                };
                await bro.setLocalStorage('PRODUCTS_ecomSession', JSON.stringify(savedLsSessionData));

                await bro.refresh();
                await bro.yaCheckMetrikaGoal(goalParams);
                const goals = await bro.yaGetMetrikaGoals(goalParams);
                assert.equal(goals.length, 1, `Должна быть отправлена цель "${goalParams.name}"`);
            });
        });
    });

    it('Товарная галерея', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=xbox&exp_flags=with_rich_media_gallery=1');
        await bro.yaWaitForVisible('.SearchPage-RMGScroller', 'нет товарной галереи');

        await bro.yaAssertViewportView('with-product-gallery');
    });
});
