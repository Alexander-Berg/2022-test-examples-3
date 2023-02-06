import {assert} from 'chai';
import moment from 'moment';
import {serp} from 'suites/avia';

import {AviaSearchResultsDesktopPage} from 'helpers/project/avia/pages';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';

describe(serp.name, () => {
    it('Внешний вид поисковой выдачи', async function () {
        const app = new TestAviaApp(this.browser);

        await app.goToIndexPage();

        await app.indexPage.search({
            fromName: 'Екатеринбург',
            toName: 'Москва',
            when: moment().add(1, 'day').format('YYYY-MM-DD'),
        });

        const searchPage = new AviaSearchResultsDesktopPage(this.browser);

        await searchPage.waitForSearchComplete();

        // filters
        assert(
            await searchPage.filters.noTransfers.isDisplayed(),
            'не отображается фильтр "Без пересадок"',
        );
        assert(
            await searchPage.filters.baggage.isDisplayed(),
            'не отображается фильтр "С багажом"',
        );
        assert(
            await searchPage.filters.transfers.isDisplayed(),
            'не отображается фильтр "Пересадки"',
        );
        assert(
            await searchPage.filters.time.isDisplayed(),
            'не отображается фильтр "Время вылета / прилёта"',
        );
        assert(
            await searchPage.filters.airports.isDisplayed(),
            'не отображается фильтр "Аэропорты"',
        );
        assert(
            await searchPage.filters.companies.isDisplayed(),
            'не отображается фильтр "Авиакомпании"',
        );
        assert(
            await searchPage.filters.partners.isDisplayed(),
            'не отображается фильтр "Партнёры"',
        );

        // sorting
        assert(
            await searchPage.filters.sorting.isDisplayed(),
            'не отображается фильтр изменения типа сортировки',
        );

        // common
        assert(
            await searchPage.subscriptionAndDynamicAsButtons.isDisplayed(),
            'не отображается блок Динамики цен и подписок',
        );
        // assert(
        //     await searchPage.direct.isDisplayed(),
        //     'не отображается блок рекламы',
        // );

        await searchPage.crossSaleMap.scrollIntoView();
        await searchPage.crossSaleMap.waitForLoading();

        assert.isTrue(
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.isVisible(),
            'Карта кросс-сейла не отобразилась',
        );
        assert.isTrue(
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.hasActiveHotelCard(),
            'Нет карточки отеля в кросс-сейле',
        );

        // variants
        const variants = await searchPage.variants.items;

        for (let i = 0; i < variants.length; i++) {
            const variant = variants[i];

            assert(
                await variant.price.isDisplayed(),
                `у ${i} сегмента нет цены`,
            );
            assert(
                await variant.buyButton.isDisplayed(),
                `у ${i} сегмента нет кнопки перехода к партнёру`,
            );
            assert(
                await variant.orderLink.isDisplayed(),
                `у ${i} сегмента нет ссылки на покупку`,
            );

            assert(
                await Promise.race(
                    variant.airlineLogos.map(logo => logo.isDisplayed()),
                ),
                `У ${i} сегмента не отображается иконка авиакомпаний`,
            );

            assert(
                await Promise.race(
                    variant.airlineTitles.map(title => title.isDisplayed()),
                ),
                `у ${i} сегмента нет названия авиакомпании`,
            );

            const forwardFlightInfo = await variant.getForwardFlightInfo();

            assert.isNotEmpty(
                forwardFlightInfo.departureTime,
                `у ${i} сегмента не отображается время отправления`,
            );
            assert.isNotEmpty(
                forwardFlightInfo.arrivalTime,
                `у ${i} сегмента не отображается время прибытия`,
            );
            assert.isNotEmpty(
                forwardFlightInfo.duration,
                `у ${i} сегмента не отображается время в пути`,
            );
            assert.isNotEmpty(
                forwardFlightInfo.departure,
                `у ${i} сегмента не отображается аэропорт отправления`,
            );
            assert.isNotEmpty(
                forwardFlightInfo.arrival,
                `у ${i} сегмента не отображается аэропорт прибытия`,
            );

            const isBaggageDisplayed =
                (await variant.carryOnIcon?.isDisplayed()) &&
                (await variant.baggageIcon?.isDisplayed());

            assert(
                isBaggageDisplayed,
                `у ${i} сегмента не отображается информация о багаже`,
            );
        }
    });
});
