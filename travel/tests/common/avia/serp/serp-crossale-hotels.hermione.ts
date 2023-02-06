import {crossSale} from 'suites/komod';
import {assert} from 'chai';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import {IAviaSearchParams} from 'helpers/project/avia/pages/SearchResultsPage/SearchResultsPage';
import TestApp from 'helpers/project/TestApp';

describe(crossSale.name, () => {
    it('Карта отелей на выдаче авиа', async function () {
        const START_DATE = moment().add(1, 'month').date(5);
        const PARAMS_TEST_CASES = [
            {endDate: null},
            {endDate: moment().add(1, 'month').date(7)},
        ];

        for (const paramsTestCase of PARAMS_TEST_CASES) {
            const testApp = new TestApp(this.browser);

            const {aviaApp} = testApp;

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

            const expectedEndDate = paramsTestCase.endDate
                ? paramsTestCase.endDate
                : moment().add(1, 'month').date(6);

            await crossSaleMap.scrollIntoView();
            await crossSaleMap.waitForLoading();

            assert.isTrue(
                await crossSaleMap.mapCard.crossSaleMap.map.isVisible(),
                'Должна отображаться кроссейл карта',
            );

            if (searchPage.isDesktop) {
                assert.isTrue(
                    await crossSaleMap.mapCard.crossSaleMap.map.hasActiveHotelCard(),
                    'Должна отображаться карточка отеля в кроссейле',
                );
            }

            const crossaleDatesOneNight = await crossSaleMap.mapCard.getDates();

            assert.equal(
                crossaleDatesOneNight.startDate.format(dateFormats.HUMAN),
                START_DATE.format(dateFormats.HUMAN),
                'Дата заезда в шапке кроссейла должна совпадать с датой вылета в поиске авиа',
            );

            assert.equal(
                crossaleDatesOneNight.endDate.format(dateFormats.HUMAN),
                expectedEndDate.format(dateFormats.HUMAN),
                'Дата выезда в шапке кроссейла должна совпадать с датой обратно в поиске авиа',
            );
        }
    });
});
