import {assert} from 'chai';
import {serp} from 'suites/avia';
import moment from 'moment';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import dateFormats from 'helpers/utilities/date/formats';
import {IAviaSearchParams} from 'helpers/project/avia/pages/SearchResultsPage/SearchResultsPage';

const DATE = moment().add(1, 'day');
const SEARCH_PARAMS: IAviaSearchParams = {
    from: {name: 'Екатеринбург', id: 'c54'},
    to: {name: 'Москва', id: 'c213'},
    startDate: DATE.format(dateFormats.ROBOT),
    travellers: {
        adults: 1,
        children: 0,
        infants: 0,
    },
    klass: 'economy',
};

describe(serp.name, () => {
    it('Общий вид выдачи в таче', async function () {
        const app = new TestAviaApp(this.browser);

        const searchPage = await app.goToSearchPage(SEARCH_PARAMS);

        await searchPage.waitForSearchComplete();

        assert.isTrue(
            await searchPage.header.portalLogo.isDisplayed(),
            'Должен отображаться логотип Яндекс Путешествий',
        );

        assert.isTrue(
            await searchPage.header.userInfo.favoriteLink.isDisplayed(),
            'Должна отображаться иконка избранных',
        );

        assert.isTrue(
            await searchPage.header.navigationSideSheet.toggleButton.isDisplayed(),
            'Должна отображаться кнопка бургерного меню',
        );

        await searchPage.header.navigationSideSheet.toggleButton.click();

        assert.isTrue(
            await searchPage.header.navigationSideSheet.user.loginButton.isDisplayed(),
            'Должна отображаться кнопка Войти в бургерном меню',
        );

        await searchPage.header.navigationSideSheet.close();

        await searchPage.header.openSearchForm();

        assert.equal(
            await searchPage.searchForm.periodDatePicker.startTrigger.value.getText(),
            DATE.format(dateFormats.HUMAN),
            'Должна совпадать дата поиска с введённой',
        );

        assert.equal(
            await searchPage.searchForm.fromSuggest.getInputValue(),
            'Екатеринбург',
            'Должен совпадать пункт отправления с введённым',
        );

        assert.equal(
            await searchPage.searchForm.toSuggest.getInputValue(),
            'Москва',
            'Должен совпадать пункт прибытия с введённым',
        );

        await searchPage.searchForm.travellers.trigger.click();

        assert.equal(
            await searchPage.searchForm.travellers.trigger.getText(),
            '1 пассажир\nэконом-класс',
            'Должны совпадать класс и количество пассажиров с введённым',
        );

        await searchPage.searchForm.travellers.modal.clickCompleteButton();

        await searchPage.searchForm.testModal.closeButton.click();

        await searchPage.waitForSearchComplete();

        assert.isTrue(
            await searchPage.touchFiltersBlock.isDisplayed(),
            'Должен присутствовать блок фильтров',
        );

        assert.isTrue(
            await searchPage.sorting.order.isDisplayed(),
            'Должен присутствовать блок сортировок',
        );

        assert.isTrue(
            await searchPage.subscriptionAndDynamicAsButtons.isDisplayed(),
            'Должен отображаться блок Динамики цен и подписок',
        );

        const firstVariant = await searchPage.variants.first();

        assert.isTrue(
            await firstVariant.mobileResultVariant.badges.isDisplayed(),
            'Должны отображаться бейджи для первого сниппета',
        );

        await searchPage.crossSaleMap.scrollIntoView();
        await searchPage.crossSaleMap.waitForLoading();

        assert.isTrue(
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.isVisible(),
            'Карта кросс-сейла не отобразилась',
        );
        assert.isTrue(
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.hasMarkers(),
            'Нет маркеров отелей в карте кросс-сейла',
        );

        const variants = await searchPage.variants.items;

        for (const variant of variants) {
            assert.isTrue(
                await variant.mobileResultVariant.logo.isDisplayed(),
                'Должен отображаться логотип авиакомпании в сниппете',
            );

            assert.isTrue(
                await variant.mobileResultVariant.baggageInfo.isDisplayed(),
                'должна отображаться багажная информация в сниппете',
            );

            assert.isTrue(
                await variant.mobileResultVariant.buyButton.isDisplayed(),
                'Должна отображаться жёлтая кнопка с ценой в сниппете',
            );

            const flightInfo =
                await variant.mobileResultVariant.flights.first();

            assert.exists(
                await flightInfo.getFlightInfo(),
                'Должна отображаться информация об отправлении и прибытии',
            );
        }

        const variantsCount = await searchPage.variants.count();
        const lastVariant = await searchPage.variants.last();

        await lastVariant.scrollIntoView();

        assert.isTrue(
            variantsCount < (await searchPage.variants.count()),
            'Должны были подгрузиться новые сниппеты после скролла страницы',
        );
    });
});
