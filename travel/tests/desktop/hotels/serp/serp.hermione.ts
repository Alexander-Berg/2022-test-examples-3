import {assert} from 'chai';
import moment from 'moment';
import {serp} from 'suites/hotels';

import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';

const DELAY_BEFORE_NEXT_SEARCH = 1000;
const DELAY_SEARCH_FORM_ANIMATE = 1000;

describe(serp.name, () => {
    it('Перепоиск отелей через поисковую форму', async function () {
        const START_SEARCH_PARAMS = {
            place: 'Екатеринбург',
            geoId: 54,
            adults: 2,
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
        };
        const NEXT_SEARCH_PARAMS = {
            place: 'Москва',
            geoId: 213,
            adults: 1,
            childrenAges: [2, 5],
            checkinDate: moment().add(1, 'month').format(dateFormats.ROBOT),
            checkoutDate: moment()
                .add(1, 'month')
                .add(2, 'days')
                .format(dateFormats.ROBOT),
        };

        const app = new TestHotelsApp(this.browser);
        const hotelsSearchPage = app.searchPage;

        await app.goToSearchPage(START_SEARCH_PARAMS);

        await hotelsSearchPage.searchResults.status.waitLoading();

        const searchFormInformationForStartSearch =
            await hotelsSearchPage.getSearchFormInformation();
        const searchFormInformationByParamsForStartSearch =
            hotelsSearchPage.getSearchFormInformationByParams(
                START_SEARCH_PARAMS,
            );

        assert.deepEqual(
            searchFormInformationForStartSearch,
            searchFormInformationByParamsForStartSearch,
            'Параметры поиска в HeaderSearchInformation не совпадают с отображаемыми при первом поиске',
        );

        await this.browser.pause(DELAY_SEARCH_FORM_ANIMATE);
        await hotelsSearchPage.searchForm.fill(NEXT_SEARCH_PARAMS);
        await hotelsSearchPage.searchForm.submitForm();
        await this.browser.pause(DELAY_BEFORE_NEXT_SEARCH);

        await hotelsSearchPage.searchResults.status.waitLoading();

        const headerSearchInformationForNextSearch =
            await hotelsSearchPage.getSearchFormInformation();
        const headerSearchInformationByParamsForNextSearch =
            hotelsSearchPage.getSearchFormInformationByParams(
                NEXT_SEARCH_PARAMS,
            );

        assert.deepEqual(
            headerSearchInformationForNextSearch,
            headerSearchInformationByParamsForNextSearch,
            'Параметры поиска в HeaderSearchInformation не совпадают с отображаемыми при повторном поиске',
        );
    });
});
