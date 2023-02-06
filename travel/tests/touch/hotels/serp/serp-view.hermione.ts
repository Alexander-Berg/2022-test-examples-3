import moment from 'moment';
import {assert} from 'chai';
import {serp} from 'suites/hotels';

import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';

const SEARCH_FOR_NIGHTS_COUNT = 2;
const now = Date.now();
const SEARCH_PARAMS = {
    place: 'Екатеринбург',
    geoId: 54,
    adults: 1,
    childrenAges: [3],
    checkinDate: moment(now).add(1, 'days').format(dateFormats.ROBOT),
    checkoutDate: moment(now)
        .add(SEARCH_FOR_NIGHTS_COUNT + 1, 'days')
        .format(dateFormats.ROBOT),
};

describe(serp.name, () => {
    it('Общий вид страницы поиска отелей. Тач.', async function () {
        const app = new TestHotelsApp(this.browser);
        const hotelsSearchPage = app.searchPage;

        await app.goToSearchPage(SEARCH_PARAMS);

        await hotelsSearchPage.searchResults.status.waitLoading();

        await hotelsSearchPage.hotelsHeaderSearchInformation.searchInformation.isDisplayed();

        const headerSearchInformation =
            await hotelsSearchPage.getHeaderSearchInformation();

        const headerSearchInformationByParams =
            hotelsSearchPage.getHeaderSearchInformationByParams(SEARCH_PARAMS);

        assert.deepEqual(
            headerSearchInformation,
            headerSearchInformationByParams,
            'Название города, даты и кол-во гостей должны совпадать с запросом',
        );

        const mapListButton = await hotelsSearchPage.mapListRadioButtons;

        assert.equal(
            await mapListButton.getText(),
            'На карте',
            'Переключатель по умолчанию в состоянии "Списком"',
        );

        assert.isTrue(
            await hotelsSearchPage.filtersBar.allFiltersButton.isDisplayed(),
            'Должна быть видна кнопка фильтров',
        );

        await mapListButton.click();

        assert.equal(
            await mapListButton.getText(),
            'Списком',
            'Переключатель в состоянии "На карте"',
        );

        assert.isTrue(
            await hotelsSearchPage.filtersBar.allFiltersButton.isDisplayed(),
            'Должна быть видна кнопка фильтров',
        );

        assert.isTrue(
            await hotelsSearchPage.searchResults.hotelCards.every(
                async card => !(await card.isVisible()),
            ),
            'На странице не должно быть списка отелей при открытии карты',
        );

        assert.isTrue(
            await (await hotelsSearchPage.map.markers.items)[0].isVisible(),
            'Должна отобразиться карта',
        );

        assert.isTrue(
            await hotelsSearchPage.map.zoomInButton.isDisplayed(),
            'Должна быть видна кнопка приближения карты',
        );

        await hotelsSearchPage.map.zoomInButton.click();

        assert.isTrue(
            await hotelsSearchPage.map.zoomOutButton.isDisplayed(),
            'Должна быть видна кнопка отдаления карты',
        );

        await hotelsSearchPage.map.zoomOutButton.click();

        assert.isTrue(
            await hotelsSearchPage.map.currentGeoLocationButton.isDisplayed(),
            'Должна быть видна кнопка текущей гео позиции',
        );

        await hotelsSearchPage.map.currentGeoLocationButton.click();
    });
});
