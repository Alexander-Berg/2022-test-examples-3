import {assert} from 'chai';
import moment from 'moment';
import {serp} from 'suites/hotels';

import dateFormats from 'helpers/utilities/date/formats';
import pluralNights from 'helpers/utilities/plural/pluralNights';
import {urlAssertor} from 'helpers/project/common/urlAssertor';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';

const HOTELS_SERP_ITEMS_MAX_COUNT = 25;
const SEARCH_FOR_NIGHTS_COUNT = 2;
const SEARCH_PARAMS = {
    place: 'Екатеринбург',
    geoId: 54,
    adults: 1,
    childrenAges: [3],
    checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
    checkoutDate: moment()
        .add(SEARCH_FOR_NIGHTS_COUNT + 1, 'days')
        .format(dateFormats.ROBOT),
};

describe(serp.name, () => {
    it('Общий вид страницы поиска отелей', async function () {
        const app = new TestHotelsApp(this.browser);
        const hotelsSearchPage = app.searchPage;

        await app.goToSearchPage(SEARCH_PARAMS);

        await hotelsSearchPage.searchResults.status.waitLoading();

        // под хедером есть форма поиска
        await hotelsSearchPage.searchForm.place.trigger.waitForVisible();

        // Название города, даты и кол-во гостей в форме совпадают с выбранными на 1м шаге
        const headerSearchFormInformation =
            await hotelsSearchPage.getSearchFormInformation();
        const headerSearchInformationByParams =
            hotelsSearchPage.getSearchFormInformationByParams(SEARCH_PARAMS);

        assert.deepEqual(
            headerSearchFormInformation,
            headerSearchInformationByParams,
            'Название города, даты и кол-во гостей совпадают с выбранными на 1м шаге',
        );

        // Под шапкой присутствуют быстрофильтры и кнопка 'Все фильтры'
        await hotelsSearchPage.filtersBar.quickFilters.waitForVisible();
        await hotelsSearchPage.filtersBar.allFiltersButton.waitForVisible();

        // В урле есть параметры adults, childrenAges, checkinDate, checkoutDate, bbox, geoid, navigationToken
        await urlAssertor(this.browser, ({searchParams}) => {
            assert.equal(
                searchParams.get('adults'),
                String(SEARCH_PARAMS.adults),
                'в урле есть параметр adults',
            );
            assert.equal(
                searchParams.get('childrenAges'),
                String(SEARCH_PARAMS.childrenAges),
                'в урле есть параметр childrenAges',
            );
            assert.equal(
                searchParams.get('checkinDate'),
                SEARCH_PARAMS.checkinDate,
                'в урле есть параметр checkinDate',
            );
            assert.equal(
                searchParams.get('checkoutDate'),
                SEARCH_PARAMS.checkoutDate,
                'в урле есть параметр checkoutDate',
            );
            assert.equal(
                searchParams.get('geoId'),
                String(SEARCH_PARAMS.geoId),
                'в урле есть параметр geoId',
            );
            assert.isString(
                searchParams.get('bbox'),
                'в урле есть параметр bbox',
            );
            assert.isString(
                searchParams.get('navigationToken'),
                'в урле есть параметр navigationToken',
            );
        });

        // Справа находится карта Екатеринбурга, отображены пины с ценами, выше кнопка Развернуть карту
        await hotelsSearchPage.map.markers.items;
        await hotelsSearchPage.map.toggleButton.waitForVisible();

        // Слева отображены предложения отелей
        const hotelCards = await hotelsSearchPage.searchResults.hotelCards
            .items;

        assert.isTrue(
            hotelCards.length > 0 &&
                hotelCards.length <= HOTELS_SERP_ITEMS_MAX_COUNT,
            'Слева отображены предложения отелей, при общем количестве отелей >25 - 25 карточек, при меньшем - сколько найдено',
        );

        // На карточке указано “цена за 2 ночи”
        const firstHotelCard = hotelCards[0];
        const nightsText = await firstHotelCard.nightsText.getText();
        const expectedNightsText = pluralNights(SEARCH_FOR_NIGHTS_COUNT);

        assert.include(
            nightsText,
            expectedNightsText,
            'На карточке указано “цена за 2 ночи”',
        );

        // Внизу страницы есть кнопки Назад и Далее. По-умолчанию кнопка Назад задизейблена,
        const isPrevButtonDisabled =
            await hotelsSearchPage.searchResults.prevButton.isDisabled();

        await hotelsSearchPage.searchResults.nextButton.isVisible();

        assert.isTrue(
            isPrevButtonDisabled,
            'По-умолчанию кнопка Назад задизейблена',
        );
    });
});
