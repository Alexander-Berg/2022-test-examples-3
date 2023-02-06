import {crossSale} from 'suites/komod';
import {assert} from 'chai';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import {IAviaSearchParams} from 'helpers/project/avia/pages/SearchResultsPage/SearchResultsPage';
import TestApp from 'helpers/project/TestApp';

describe(crossSale.name, () => {
    it('Переход по кроссейл карте с выдачи авиа', async function () {
        const START_DATE = moment().add(1, 'month').date(5);
        const PARAMS_TEST_CASES = [
            {endDate: null},
            {endDate: moment().add(1, 'month').date(7)},
        ];

        for (const paramsTestCase of PARAMS_TEST_CASES) {
            const testApp = new TestApp(this.browser);

            const {aviaApp, hotelsApp} = testApp;

            const SEARCH_PARAMS: IAviaSearchParams = {
                from: {name: 'Екатеринбург', id: 'c54'},
                to: {name: 'Москва', id: 'c213'},
                startDate: START_DATE.format(dateFormats.ROBOT),
                endDate: paramsTestCase.endDate?.format(dateFormats.ROBOT),
                travellers: {
                    adults: 1,
                    children: 0,
                    infants: 0,
                },
                klass: 'economy',
            };

            const searchPage = await aviaApp.goToSearchPage(SEARCH_PARAMS);

            await searchPage.waitForSearchComplete();

            const {crossSaleMap} = searchPage;

            const expectedHotelsEndDate = paramsTestCase.endDate
                ? paramsTestCase.endDate
                : moment().add(1, 'month').date(6);

            await crossSaleMap.scrollIntoView();
            await crossSaleMap.waitForLoading();

            await crossSaleMap.mapCard.crossSaleMap.click();

            await this.browser.switchToNextTab();

            const hotelsSearchPage = hotelsApp.searchPage;

            const hotelsSearchFormDates =
                await hotelsSearchPage.getSearchFormDates();

            assert.equal(
                hotelsSearchFormDates.startDate.format(
                    dateFormats.HUMAN_SHORT_WITH_SHORT_YEAR,
                ),
                START_DATE.format(dateFormats.HUMAN_SHORT_WITH_SHORT_YEAR),
                'Дата заезда на выдаче отелей должна совпадать с датой вылета в авиа',
            );

            assert.equal(
                hotelsSearchFormDates.endDate.format(
                    dateFormats.HUMAN_SHORT_WITH_SHORT_YEAR,
                ),
                expectedHotelsEndDate.format(
                    dateFormats.HUMAN_SHORT_WITH_SHORT_YEAR,
                ),
                'Дата выезда на выдаче отелей должна быть на 1 день больше даты выезда',
            );
        }
    });
});
