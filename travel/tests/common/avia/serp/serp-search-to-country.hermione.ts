import {assert} from 'chai';
import {index, serp} from 'suites/avia';
import moment from 'moment';

import {
    TestIndexAviaPage,
    AviaSearchResultsPage,
} from 'helpers/project/avia/pages';
import dateFormats from 'helpers/utilities/date/formats';

describe(serp.name, () => {
    it('Поиск из города в страну', async function () {
        await this.browser.url(index.url);

        const indexPage = new TestIndexAviaPage(this.browser);

        await indexPage.search({
            fromName: 'Москва',
            toName: 'США',
            toPickFirst: true,
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
        });

        const searchPage = new AviaSearchResultsPage(this.browser);

        await searchPage.waitForSearchComplete();

        const firstSnippet = await searchPage.variants.first();

        const firstFlightMobile =
            await firstSnippet.mobileResultVariant.flights.first();

        if (searchPage.isTouch) {
            await firstFlightMobile.scrollIntoView();

            assert.isTrue(
                ['JFK', 'EWR', 'LGA'].includes(
                    await firstFlightMobile.toTimeBottomDescription.getText(),
                ),
                'Должна быть получена выдача для Нью-Йорка',
            );
        } else {
            assert.isTrue(
                [
                    'Кеннеди\nJFK',
                    'Ньюарк Либерти\nEWR',
                    'Ла Гуардиа\nLGA',
                ].includes(
                    await firstSnippet.desktopResultVariant.forwardFlights.arrival.getText(),
                ),
                'Должна быть получена выдача для Нью-Йорка',
            );
        }
    });
});
