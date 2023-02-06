import {assert} from 'chai';
import moment from 'moment';
import {random} from 'lodash';

import cities from 'helpers/project/trains/data/cities';
import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';
import {RailwaysSearchForm} from 'helpers/project/trains/components/RailwaysSearchForm';
import TestBusesSearchForm from 'helpers/project/buses/components/TestBusesSearchForm/TestBusesSearchForm';
import {AviaSearchForm} from 'helpers/project/avia/components/AviaSearchForm';
import {HotelsSearchForm} from 'helpers/project/hotels/components/HotelsSearchForm';

const when = moment()
    .add(1, 'month')
    .add(random(1, 7), 'day')
    .format('YYYY-MM-DD');
const countDaysBeforeCheckin = random(1, 7);
const countDaysBeforeCheckout = countDaysBeforeCheckin + random(1, 7);

async function checkTransportSuggests(
    searchForm: RailwaysSearchForm | TestBusesSearchForm | AviaSearchForm,
    isDesktop: boolean,
    browser: WebdriverIO.Browser,
): Promise<void> {
    const {fromSuggest, toSuggest} = searchForm;

    await fromSuggest.click();
    await fromSuggest.setInputValue('Моск');
    await browser.pause(1000);

    const firstFromSuggest = await fromSuggest.suggestItems.first();
    const firstFromSuggestValue = await firstFromSuggest.title.getText();

    if (isDesktop) {
        await toSuggest.click();
    } else {
        await fromSuggest.cancel();
    }

    const fromFieldValue = await fromSuggest.getTriggerValue();

    assert.equal(
        fromFieldValue,
        firstFromSuggestValue,
        `Значение в поле "откуда" (${fromFieldValue}) не совпадает с первым элементом из саджестов (${firstFromSuggestValue})`,
    );

    await toSuggest.click();
    await toSuggest.setInputValue('Санкт');
    await browser.pause(1000);

    const firstToSuggest = await toSuggest.suggestItems.first();
    const firstToSuggestValue = await firstToSuggest.title.getText();

    if (isDesktop) {
        await fromSuggest.click();
    } else {
        await toSuggest.cancel();
    }

    const toFieldValue = await toSuggest.getTriggerValue();

    assert.equal(
        toFieldValue,
        firstToSuggestValue,
        `Значение в поле "куда" (${toFieldValue}) не совпадает с первым элементом из саджестов (${firstFromSuggestValue})`,
    );
}

async function checkHotelsSuggest(
    {place, period}: HotelsSearchForm,
    isDesktop: boolean,
    browser: WebdriverIO.Browser,
): Promise<void> {
    await place.click();
    await place.setInputValue('Моск');
    await browser.pause(1000);

    const firstPlaceSuggest = await place.suggestItems.first();
    const firstPlaceSuggestValue = await firstPlaceSuggest.title.getText();

    if (isDesktop) {
        await period.click();
    } else {
        await place.cancel();
    }

    const placeFieldValue = await place.getTriggerValue();

    assert.equal(
        placeFieldValue,
        firstPlaceSuggestValue,
        `Значение в поле отеля/региона (${placeFieldValue}) не совпадает с первым элементом из саджестов (${firstPlaceSuggestValue})`,
    );
}

describe('Портал', () => {
    it('Автовыбор саджеста в форме поиска при потере фокуса', async function () {
        const aviaApp = new TestAviaApp(this.browser);
        const trainsApp = new TestTrainsApp(this.browser);
        const busesApp = new TestBusesApp(this.browser);
        const hotelsApp = new TestHotelsApp(this.browser);

        for (const app of [aviaApp, trainsApp, busesApp]) {
            await app.goToIndexPage();
            await checkTransportSuggests(
                app.indexPage.searchForm,
                app.indexPage.isDesktop,
                this.browser,
            );
        }

        await hotelsApp.goToIndexPage();
        await checkHotelsSuggest(
            hotelsApp.indexPage.searchForm,
            hotelsApp.indexPage.isDesktop,
            this.browser,
        );

        for (const {
            app: {searchPage},
            search,
            type,
        } of [
            {
                app: aviaApp,
                search: aviaApp.goToSearchPage.bind(aviaApp, {
                    from: {name: 'Санкт-Петербург', id: 'c2'},
                    to: {name: 'Екатеринбург', id: 'c54'},
                    startDate: when,
                    travellers: {
                        adults: 1,
                        children: 0,
                        infants: 0,
                    },
                    klass: 'economy',
                }),
                type: 'avia',
            },
            {
                app: trainsApp,
                search: trainsApp.goToSearchPage.bind(
                    trainsApp,
                    cities.spb.slug,
                    cities.ekb.slug,
                    when,
                    true,
                ),
                type: 'trains',
            },
            {
                app: busesApp,
                search: busesApp.goToFilledSearchPage.bind(busesApp),
                type: 'buses',
            },
        ]) {
            await search();

            if (
                !(
                    (type === 'avia' ||
                        type === 'trains' ||
                        type === 'buses') &&
                    searchPage.isDesktop
                )
            ) {
                await searchPage.header.openSearchForm();
            }

            await checkTransportSuggests(
                searchPage.searchForm,
                searchPage.isDesktop,
                this.browser,
            );
        }

        await hotelsApp.goToSearchPage({
            place: 'Санкт-Петербург',
            geoId: 2,
            adults: 1,
            childrenAges: [3],
            checkinDate: moment()
                .add(countDaysBeforeCheckin, 'days')
                .format(dateFormats.ROBOT),
            checkoutDate: moment()
                .add(countDaysBeforeCheckout, 'days')
                .format(dateFormats.ROBOT),
        });

        if (hotelsApp.searchPage.isTouch) {
            await hotelsApp.searchPage.hotelsHeaderSearchInformation.openSearchForm();
        }

        await checkHotelsSuggest(
            hotelsApp.searchPage.searchForm,
            hotelsApp.searchPage.isDesktop,
            this.browser,
        );
    });
});
