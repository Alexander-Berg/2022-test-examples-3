import {index} from 'suites/avia';
import moment from 'moment';
import {assert} from 'chai';

import {SECOND} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';
import TestBusesIndexPage from 'helpers/project/buses/pages/TestBusesIndexPage/TestBusesIndexPage';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';

describe('Портал: Главная', () => {
    it('[Десктоп] Портал. Ввод в поля формы поиска. Авиа', async function () {
        const testApp = new TestApp(this.browser);
        const {aviaApp} = testApp;

        await aviaApp.goToIndexPage();

        await aviaApp.indexPage.adfoxBanner.disableEvents();

        await aviaApp.indexPage.searchForm.fromSuggest.resetValue();

        const suggestFrom =
            await aviaApp.indexPage.searchForm.fromSuggest.suggestItems.first();

        await suggestFrom.click();

        assert.isFalse(
            await aviaApp.indexPage.searchForm.fromSuggest.suggestItems.isDisplayed(),
            'Попап с саджестами поля Откуда должен скрыться',
        );

        const suggestTo =
            await aviaApp.indexPage.searchForm.toSuggest.suggestItems.first();

        await suggestTo.click();

        assert.isFalse(
            await aviaApp.indexPage.searchForm.toSuggest.suggestItems.isDisplayed(),
            'Попап с саджестами поля Куда должен скрыться',
        );

        await aviaApp.indexPage.searchForm.periodDatePicker.calendar.clickCalendarDate(
            moment().add(3, 'day').format(dateFormats.ROBOT),
        );

        await aviaApp.indexPage.searchForm.periodDatePicker.calendar.clickCalendarDate(
            moment().add(5, 'day').format(dateFormats.ROBOT),
        );

        assert.isFalse(
            await aviaApp.indexPage.searchForm.periodDatePicker.calendar.isDisplayed(),
            'Попап с календарем должен скрыться',
        );

        const arrivalCityAviaIndexPage =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureCityAviaIndexPage =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureDateAviaIndexPage =
            await aviaApp.indexPage.searchForm.periodDatePicker.startTrigger.value.getText();

        const returnDateAviaIndexPage =
            await aviaApp.indexPage.searchForm.periodDatePicker.endTrigger.value.getText();

        await aviaApp.indexPage.searchForm.submitForm();

        await aviaApp.searchPage.header.waitForVisible(10 * SECOND);

        if (aviaApp.searchPage.isTouch) {
            await aviaApp.searchPage.header.openSearchForm();
        }

        const arrivalCityAviaSearchPage =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureCityAviaSearchPage =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureDateAviaSearchPage =
            await aviaApp.indexPage.searchForm.periodDatePicker.startTrigger.value.getText();

        const returnDateAviaSearchPage =
            await aviaApp.indexPage.searchForm.periodDatePicker.endTrigger.value.getText();

        assert.equal(
            arrivalCityAviaSearchPage,
            arrivalCityAviaIndexPage,
            'Город отправления на выдаче соответствует городу отправления на главной',
        );
        assert.equal(
            departureCityAviaSearchPage,
            departureCityAviaIndexPage,
            'Город прибытия на выдаче соответствует городу прибытия на главной',
        );
        assert.equal(
            departureDateAviaSearchPage,
            departureDateAviaIndexPage,
            'Дата отправления на выдаче соответствует дате отправления на главной',
        );
        assert.equal(
            returnDateAviaSearchPage,
            returnDateAviaIndexPage,
            'Дата обратно на выдаче соответствует дате обратно на главной',
        );
    });

    it('[Десктоп] Портал. Ввод в поля формы поиска. ЖД', async function () {
        const testApp = new TestApp(this.browser);
        const {trainsApp} = testApp;

        await trainsApp.goToIndexPage();
        await trainsApp.indexPage.adfoxBanner.disableEvents();

        await trainsApp.indexPage.searchForm.fromSuggest.resetValue();

        const suggestFrom =
            await trainsApp.indexPage.searchForm.fromSuggest.suggestItems.first();

        await suggestFrom.click();

        assert.isFalse(
            await trainsApp.indexPage.searchForm.fromSuggest.suggestItems.isDisplayed(),
            'Попап с саджестами поля Откуда должен скрыться',
        );

        const suggestTo =
            await trainsApp.indexPage.searchForm.toSuggest.suggestItems.first();

        await suggestTo.click();

        assert.isFalse(
            await trainsApp.indexPage.searchForm.toSuggest.suggestItems.isDisplayed(),
            'Попап с саджестами поля Куда должен скрыться',
        );
        await trainsApp.indexPage.searchForm.whenDatePicker.calendar.waitScrollAnimation();
        await trainsApp.indexPage.searchForm.whenDatePicker.calendar.clickCalendarDate(
            moment().add(3, 'day').format(dateFormats.ROBOT),
        );

        assert.isFalse(
            await trainsApp.indexPage.searchForm.whenDatePicker.calendar.isDisplayed(),
            'Попап с календарем должен скрыться',
        );

        await trainsApp.indexPage.searchForm.whenDatePicker.endTrigger.click();
        await trainsApp.indexPage.searchForm.whenDatePicker.calendar.waitScrollAnimation();
        await trainsApp.indexPage.searchForm.whenDatePicker.calendar.clickCalendarDate(
            moment().add(5, 'day').format(dateFormats.ROBOT),
        );

        assert.isFalse(
            await trainsApp.indexPage.searchForm.whenDatePicker.calendar.isDisplayed(),
            'Попап с календарем должен скрыться',
        );

        const arrivalCityTrainsIndexPage =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureCityTrainsIndexPage =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureDateTrainsIndexPage =
            await trainsApp.indexPage.searchForm.whenDatePicker.startTrigger.value.getText();

        const returnDateTrainsIndexPage =
            await trainsApp.indexPage.searchForm.whenDatePicker.endTrigger.value.getText();

        await trainsApp.indexPage.searchForm.submitForm();

        await trainsApp.searchPage.waitVariantsLoaded();

        const arrivalCityTrainsSearchPage =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureCityTrainsSearchPage =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureDateTrainsSearchPage =
            await trainsApp.indexPage.searchForm.whenDatePicker.startTrigger.value.getText();

        const returnDateTrainsSearchPage =
            await trainsApp.indexPage.searchForm.whenDatePicker.endTrigger.value.getText();

        assert.equal(
            arrivalCityTrainsSearchPage,
            arrivalCityTrainsIndexPage,
            'Город отправления на выдаче соответствует городу отправления на главной',
        );
        assert.equal(
            departureCityTrainsSearchPage,
            departureCityTrainsIndexPage,
            'Город прибытия на выдаче соответствует городу прибытия на главной',
        );
        assert.equal(
            departureDateTrainsSearchPage,
            departureDateTrainsIndexPage,
            'Дата отправления на выдаче соответствует дате отправления на главной',
        );
        assert.equal(
            returnDateTrainsSearchPage,
            returnDateTrainsIndexPage,
            'Дата обратно на выдаче соответствует дате обратно на главной',
        );
    });

    it('[Десктоп] Портал. Ввод в поля формы поиска. Отели', async function () {
        await this.browser.url(index.url);

        const testApp = new TestApp(this.browser);
        const {hotelsApp} = testApp;

        await hotelsApp.goToIndexPage();
        await hotelsApp.indexPage.adfoxBanner.disableEvents();

        await hotelsApp.indexPage.searchForm.place.click();

        const suggestPlace =
            await hotelsApp.indexPage.searchForm.place.suggestItems.first();

        await suggestPlace.click();

        assert.isFalse(
            await hotelsApp.indexPage.searchForm.place.suggestItems.isDisplayed(),
            'Попап с саджестами поля Куда должен скрыться',
        );

        await hotelsApp.indexPage.searchForm.period.calendar.clickCalendarDate(
            moment().add(3, 'day').format(dateFormats.ROBOT),
        );

        await hotelsApp.indexPage.searchForm.period.calendar.clickCalendarDate(
            moment().add(5, 'day').format(dateFormats.ROBOT),
        );

        if (hotelsApp.indexPage.isDesktop) {
            await this.browser.waitUntil(
                async () => {
                    return !(await hotelsApp.indexPage.searchForm.period.calendar.isDisplayed());
                },
                {
                    timeout: 5 * SECOND,
                    timeoutMsg: 'Календарь должен скрыться',
                },
            );
        }

        const placeIndexPage =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

        const checkinDateIndexPage =
            await hotelsApp.indexPage.searchForm.period.startTrigger.value.getText();

        const checkoutDateIndexPage =
            await hotelsApp.indexPage.searchForm.period.endTrigger.value.getText();

        await hotelsApp.indexPage.searchForm.submitForm();

        const placeSearchPage =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

        const checkinDateSearchPage =
            await hotelsApp.indexPage.searchForm.period.startTrigger.value.getText();

        const checkoutDateSearchPage =
            await hotelsApp.indexPage.searchForm.period.endTrigger.value.getText();

        assert.equal(
            placeSearchPage,
            placeIndexPage,
            'Город на выдаче соответствует городу на главной',
        );
        assert.equal(
            checkinDateSearchPage,
            checkinDateIndexPage,
            'Дата заселения на выдаче соответствует дате заселения на главной',
        );
        assert.equal(
            checkoutDateSearchPage,
            checkoutDateIndexPage,
            'Дата выселения на выдаче соответствует дате выселения на главной',
        );
    });

    it('[Десктоп] Портал. Ввод в поля формы поиска. Автобусы', async function () {
        await this.browser.url(index.url);

        const app = new TestBusesApp(this.browser);
        const indexPage = new TestBusesIndexPage(this.browser);

        await app.goToIndexPage();
        await indexPage.adfoxBanner.disableEvents();
        await indexPage.searchForm.fromSuggest.resetButton.click();

        const suggestFrom =
            await indexPage.searchForm.fromSuggest.suggestItems.first();

        await suggestFrom.click();

        assert.isFalse(
            await indexPage.searchForm.fromSuggest.suggestItems.isDisplayed(),
            'Попап с саджестами поля Откуда должен скрыться',
        );

        const suggestTo =
            await indexPage.searchForm.toSuggest.suggestItems.first();

        await suggestTo.click();

        await indexPage.searchForm.datePicker.calendar.clickCalendarDate(
            moment().add(3, 'day').format(dateFormats.ROBOT),
        );

        assert.isFalse(
            await indexPage.searchForm.datePicker.calendar.isDisplayed(),
            'Попап с календарем должен скрыться',
        );

        const arrivalCityBusIndexPage =
            await indexPage.searchForm.toSuggest.getInputValue();

        const departureCityBusIndexPage =
            await indexPage.searchForm.fromSuggest.getInputValue();

        const dateIndexPage =
            await indexPage.searchForm.datePicker.startTrigger.value.getText();

        await indexPage.searchForm.submitButton.click();

        const searchPage = app.searchPage;

        const departureCityBusSearchPage =
            await searchPage.searchForm.fromSuggest.getInputValue();

        const arrivalCityBusSearchPage =
            await searchPage.searchForm.toSuggest.getInputValue();

        const dateSearchPage =
            await searchPage.searchForm.datePicker.startTrigger.value.getText();

        assert.equal(
            departureCityBusSearchPage,
            departureCityBusIndexPage,
            'Город отправления на выдаче соответствует городу отправления на главной',
        );
        assert.equal(
            arrivalCityBusSearchPage,
            arrivalCityBusIndexPage,
            'Город прибытия на выдаче соответствует городу прибытия на главной',
        );
        assert.equal(
            dateSearchPage,
            dateIndexPage,
            'Дата на выдаче соответствует дате на главной',
        );
    });
});
