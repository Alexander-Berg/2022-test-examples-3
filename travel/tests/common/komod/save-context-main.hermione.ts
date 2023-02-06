import {index} from 'suites/avia';
import {context} from 'suites/komod';
import moment from 'moment';
import {assert} from 'chai';

import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';

describe(context.name, () => {
    it('Сохранение контекста с главной на главную - авиа-жд', async function () {
        await this.browser.url(index.url);

        const testApp = new TestApp(this.browser);
        const {aviaApp, trainsApp} = testApp;

        await aviaApp.indexPage.searchForm.fill({
            fromName: 'Шереметьево',
            toName: 'Санкт-Петербург',
            toPickFirst: true,
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
            return_date: moment().add(5, 'day').format(dateFormats.ROBOT),
        });

        const departureDateAvia =
            await aviaApp.indexPage.searchForm.periodDatePicker.startTrigger.value.getText();

        const returnDateAvia =
            await aviaApp.indexPage.searchForm.periodDatePicker.endTrigger.value.getText();

        const arrivalCityAvia =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        await aviaApp.indexPage.indexTabs.trainsTab.click();

        await trainsApp.indexPage.searchForm.waitForVisible(3000);

        const departureDateTrains =
            await trainsApp.indexPage.searchForm.whenDatePicker.startTrigger.value.getText();

        assert.equal(
            departureDateAvia,
            departureDateTrains,
            'Дата вылета в авиа должна совпадать с датой выезда в жд',
        );

        const returnDateTrains =
            await trainsApp.indexPage.searchForm.whenDatePicker.endTrigger.value.getText();

        assert.equal(
            returnDateAvia,
            returnDateTrains,
            'Дата обратно в авиа должна совпадать с датой обратно в жд',
        );

        const departureCityTrains =
            await trainsApp.indexPage.searchForm.fromSuggest.getInputValue();

        assert.isTrue(
            departureCityTrains === 'Москва',
            'Город отправления в жд должен соответствовать указанному аэропорту в авиа',
        );

        const arrivalCityTrains =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        assert.equal(
            arrivalCityAvia,
            arrivalCityTrains,
            'Город прибытия в авиа должен совпадать с городом прибытия в жд',
        );
    });
    it('Сохранение контекста с главной на главную - авиа-отели', async function () {
        await this.browser.url(index.url);

        const testApp = new TestApp(this.browser);
        const {aviaApp, hotelsApp} = testApp;

        await aviaApp.indexPage.searchForm.fill({
            fromName: 'Москва',
            toName: 'Санкт-Петербург',
            toPickFirst: true,
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
            return_date: moment().add(5, 'day').format(dateFormats.ROBOT),
        });

        const arrivalCityAvia =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        await aviaApp.indexPage.indexTabs.hotelsTab.click();

        const cityHotels =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

        assert.equal(
            arrivalCityAvia,
            cityHotels,
            'Город в отелях должен совпадать с городом прибытия в авиа',
        );

        if (hotelsApp.indexPage.isDesktop) {
            assert.isTrue(
                await hotelsApp.indexPage.searchForm.period.calendar.isDisplayed(),
                'Календарь должен быть раскрыт',
            );
        }
    });

    it('Сохранение контекста с главной на главную - жд-отели', async function () {
        await this.browser.url(index.url);

        const testApp = new TestApp(this.browser);
        const {hotelsApp, trainsApp} = testApp;

        await trainsApp.goToIndexPage();

        await trainsApp.indexPage.searchForm.fill({
            from: 'Москва',
            to: 'Санкт-Петербург',
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
        });

        const arrivalCityTrains =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        await trainsApp.indexPage.indexTabs.hotelsTab.click();

        const cityHotels =
            await hotelsApp.indexPage.searchForm.place._input.getValue();

        assert.equal(
            arrivalCityTrains,
            cityHotels,
            'Город в отелях должен совпадать с городом прибытия в жд',
        );

        if (hotelsApp.indexPage.isDesktop) {
            assert.isTrue(
                await hotelsApp.indexPage.searchForm.period.calendar.isDisplayed(),
                'Календарь должен быть раскрыт',
            );
        }
    });
    it('Сохранение контекста с главной на главную - отели-авиа', async function () {
        await this.browser.url(index.url);

        const testApp = new TestApp(this.browser);
        const {aviaApp, hotelsApp} = testApp;

        await hotelsApp.goToIndexPage();

        await hotelsApp.indexPage.searchForm.fill({
            checkinDate: moment().add(3, 'day').format(dateFormats.ROBOT),
            checkoutDate: moment().add(6, 'day').format(dateFormats.ROBOT),
            place: 'Казань',
        });

        await hotelsApp.indexPage.indexTabs.aviaTab.click();

        const departureCityAvia =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        assert.isTrue(
            departureCityAvia === 'Казань',
            'Город прибытия в авиа должен совпадать с городом в отелях',
        );

        if (aviaApp.indexPage.isDesktop) {
            assert.isTrue(
                await aviaApp.indexPage.searchForm.periodDatePicker.isDisplayed(),
                'Календарь в авиа должен быть раскрыт',
            );
        }
    });
});
