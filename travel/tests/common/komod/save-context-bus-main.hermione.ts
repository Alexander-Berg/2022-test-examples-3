import {context} from 'suites/komod';
import {assert} from 'chai';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';

describe(context.name, () => {
    it('Сохранение контекста с главной на главную - автобусы-жд', async function () {
        const testApp = new TestApp(this.browser);
        const {trainsApp, busesApp} = testApp;

        await busesApp.goToIndexPage();

        await busesApp.indexPage.searchForm.fill({
            from: 'Москва',
            to: 'Санкт-Петербург',
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
        });

        const departureCityBuses =
            await busesApp.indexPage.searchForm.fromSuggest.getInputValue();
        const arrivalCityBuses =
            await busesApp.indexPage.searchForm.toSuggest.getInputValue();
        const dateBuses =
            await busesApp.indexPage.searchForm.datePicker.getStartDate();

        await busesApp.indexPage.indexTabs.trainsTab.click();

        await trainsApp.indexPage.searchForm.waitForVisible(3000);

        const departureCityTrains =
            await trainsApp.indexPage.searchForm.fromSuggest.getInputValue();

        assert.equal(
            departureCityTrains,
            departureCityBuses,
            'Город отправления в жд должен совпадать с городом отправления в автобусах',
        );

        const arrivalCityTrains =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        assert.equal(
            arrivalCityTrains,
            arrivalCityBuses,
            'Город прибытия в жд должен совпадать с городом прибытия в автобусах',
        );

        const departureDateTrains =
            await trainsApp.indexPage.searchForm.whenDatePicker.getStartDate();

        assert.equal(
            departureDateTrains.format(dateFormats.HUMAN),
            dateBuses.format(dateFormats.HUMAN),
            'Дата отправления в жд должна совпадать с датой в автобусах',
        );
    });

    it('Сохранение контекста с главной на главную - автобусы-отели', async function () {
        const testApp = new TestApp(this.browser);
        const {hotelsApp, busesApp} = testApp;

        await busesApp.goToIndexPage();

        await busesApp.indexPage.searchForm.fill({
            from: 'Москва',
            to: 'Санкт-Петербург',
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
        });

        const arrivalCityBuses =
            await busesApp.indexPage.searchForm.toSuggest.getInputValue();

        await busesApp.indexPage.indexTabs.hotelsTab.click();

        const cityHotels =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

        assert.equal(
            cityHotels,
            arrivalCityBuses,
            'Город в отелях должен совпадать с городом прибытия в автобусах',
        );

        if (hotelsApp.indexPage.isDesktop) {
            assert.isTrue(
                await hotelsApp.indexPage.searchForm.period.calendar.waitForVisible(),
                'Календарь должен быть раскрыт',
            );
        }
    });

    it('Сохранение контекста с главной на главную - отели-автобусы', async function () {
        const testApp = new TestApp(this.browser);
        const {hotelsApp, busesApp} = testApp;

        await hotelsApp.goToIndexPage();

        await hotelsApp.indexPage.searchForm.fill({
            checkinDate: moment().add(3, 'day').format(dateFormats.ROBOT),
            checkoutDate: moment().add(6, 'day').format(dateFormats.ROBOT),
            place: 'Казань',
        });

        const cityHotels =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

        await hotelsApp.indexPage.indexTabs.busesTab.click();

        await busesApp.indexPage.searchForm.waitForVisible(5000);

        const departureCityBuses =
            await busesApp.indexPage.searchForm.toSuggest.getInputValue();

        assert.equal(
            departureCityBuses,
            cityHotels,
            'Город прибытия в автобусах должен совпадать с городом в отелях',
        );

        if (busesApp.indexPage.isDesktop) {
            assert.isTrue(
                await busesApp.indexPage.searchForm.datePicker.waitForVisible(),
                'Календарь в автобусах должен быть раскрыт',
            );
        }
    });

    it('Сохранение контекста с главной на главную - авиа-автобусы', async function () {
        const testApp = new TestApp(this.browser);
        const {aviaApp, busesApp} = testApp;

        await aviaApp.goToIndexPage();

        await aviaApp.indexPage.searchForm.fill({
            fromName: 'Екатеринбург',
            toName: 'Шереметьево',
            toPickFirst: true,
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
            return_date: moment().add(5, 'day').format(dateFormats.ROBOT),
        });

        const departureCityAvia =
            await aviaApp.indexPage.searchForm.fromSuggest.getInputValue();

        const departureDateAvia =
            await aviaApp.indexPage.searchForm.periodDatePicker.getStartDate();

        await aviaApp.indexPage.indexTabs.busesTab.click();

        await busesApp.indexPage.searchForm.waitForVisible(5000);

        const departureCityBuses =
            await busesApp.indexPage.searchForm.fromSuggest.getInputValue();

        assert.equal(
            departureCityBuses,
            departureCityAvia,
            'Город отправления в автобусах должен совпадать с городом отправления в авиа',
        );

        const arrivalCityBuses =
            await busesApp.indexPage.searchForm.toSuggest.getInputValue();

        assert.isTrue(
            arrivalCityBuses === 'Москва',
            'Город прибытия в автобусах должен соответствовать указанному аэропорту в авиа',
        );

        const departureDateBuses =
            await busesApp.indexPage.searchForm.datePicker.getStartDate();

        assert.equal(
            departureDateBuses.format(dateFormats.HUMAN),
            departureDateAvia.format(dateFormats.HUMAN),
            'Дата отправления в автобусах должна совпадать с датой отправления в авиа',
        );
    });
});
