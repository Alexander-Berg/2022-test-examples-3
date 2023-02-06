import {context} from 'suites/komod';
import moment from 'moment';
import {assert} from 'chai';
import {random} from 'lodash';

import {SECOND} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';

describe(context.name, () => {
    it('Сохранение контекста с выдачи на главную - авиа-жд', async function () {
        const testApp = new TestApp(this.browser);
        const {aviaApp, trainsApp} = testApp;

        await aviaApp.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Санкт-Петербург', id: 'c2'},
            startDate: moment()
                .add(1, 'months')
                .add(random(1, 10), 'day')
                .format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
        });

        await aviaApp.searchPage.waitForSearchComplete();

        if (aviaApp.searchPage.isTouch) {
            await aviaApp.searchPage.header.openSearchForm();
        }

        await aviaApp.searchPage.searchForm.fill({
            fromName: 'Кольцово',
            toName: 'Казань',
            toPickFirst: true,
            when: moment().add(4, 'day').format(dateFormats.ROBOT),
            return_date: moment().add(6, 'day').format(dateFormats.ROBOT),
        });

        const departureDateAvia =
            await aviaApp.searchPage.searchForm.periodDatePicker.startTrigger.value.getText();

        const returnDateAvia =
            await aviaApp.searchPage.searchForm.periodDatePicker.endTrigger.value.getText();

        const arrivalCityAvia =
            await aviaApp.searchPage.searchForm.toSuggest.getInputValue();

        if (aviaApp.searchPage.isTouch) {
            await aviaApp.searchPage.searchForm.testModal.closeButton.click();
            await aviaApp.searchPage.header.navigationSideSheet.toggleButton.click();
            await aviaApp.searchPage.header.navigationSideSheet.trainsLink.click();
        } else {
            await aviaApp.searchPage.header.navigations.trains.click();
        }

        const departureDateTrains =
            await trainsApp.searchPage.searchForm.whenDatePicker.startTrigger.value.getText();

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
            departureCityTrains === 'Екатеринбург',
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

    it('Сохранение контекста с выдачи на главную - жд-авиа', async function () {
        const testApp = new TestApp(this.browser);
        const {aviaApp, trainsApp} = testApp;

        await trainsApp.goToIndexPage();

        await trainsApp.indexPage.searchForm.fill({
            from: 'Москва',
            to: 'Санкт-Петербург',
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
        });

        await trainsApp.indexPage.searchForm.submitForm();
        await trainsApp.searchPage.waitVariantsLoaded();

        if (trainsApp.searchPage.isTouch) {
            await trainsApp.searchPage.portalHeader.openSearchForm();
        }

        await trainsApp.searchPage.searchForm.fill({
            from: 'Екатеринбург',
            to: 'Казань',
            when: moment().add(5, 'day').format(dateFormats.ROBOT),
        });

        const arrivalCityTrains =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();
        const departureCityTrains =
            await trainsApp.indexPage.searchForm.fromSuggest.getInputValue();
        const departureDateTrains =
            await trainsApp.indexPage.searchForm.whenDatePicker.startTrigger.value.getText();

        if (trainsApp.searchPage.isTouch) {
            await trainsApp.searchPage.searchForm.testModal.closeButton.click();
            await trainsApp.searchPage.header.navigationSideSheet.toggleButton.click();
            await trainsApp.searchPage.header.navigationSideSheet.aviaLink.click();
        } else {
            await trainsApp.searchPage.header.navigations.avia.click();
        }

        const arrivalCityAvia =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        const departureCityAvia =
            await aviaApp.indexPage.searchForm.fromSuggest.getInputValue();

        const departureDateAvia =
            await aviaApp.indexPage.searchForm.periodDatePicker.startTrigger.value.getText();

        assert.equal(
            arrivalCityTrains,
            arrivalCityAvia,
            'Город прибытия в авиа должен совпадать с городом прибытия в жд',
        );

        assert.equal(
            departureCityTrains,
            departureCityAvia,
            'Город отправления в авиа должен совпадать с городом отправления в жд',
        );

        assert.equal(
            departureDateAvia,
            departureDateTrains,
            'Дата отправления в авиа должна совпадать с датой отправления в жд',
        );
    });

    it('Сохранение контекста с выдачи на главную - авиа-отели', async function () {
        const testApp = new TestApp(this.browser);
        const {aviaApp, hotelsApp} = testApp;

        await aviaApp.goToIndexPage();

        const {indexPage} = aviaApp;

        await indexPage.searchForm.fill({
            fromName: 'Москва',
            toName: 'Санкт-Петербург',
            when: moment()
                .add(1, 'months')
                .add(random(1, 10), 'day')
                .format(dateFormats.ROBOT),
        });
        await indexPage.searchForm.submitForm();

        await aviaApp.searchPage.waitForSearchComplete();

        if (aviaApp.searchPage.isTouch) {
            await aviaApp.searchPage.header.openSearchForm();
        }

        await aviaApp.searchPage.searchForm.fill({
            fromName: 'Екатеринбург',
            toName: 'Казань',
            toPickFirst: true,
            when: moment().add(4, 'day').format(dateFormats.ROBOT),
            return_date: moment().add(6, 'day').format(dateFormats.ROBOT),
        });

        const arrivalCityAvia =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        if (aviaApp.searchPage.isTouch) {
            await aviaApp.searchPage.searchForm.testModal.closeButton.click();
            await aviaApp.searchPage.header.navigationSideSheet.toggleButton.click();
            await aviaApp.searchPage.header.navigationSideSheet.hotelsLink.click();
        } else {
            await aviaApp.searchPage.header.navigations.hotels.click();
        }

        await this.browser.waitUntil(
            async () => {
                const value =
                    await hotelsApp.indexPage.searchForm.place.getInputValue();

                return Boolean(value);
            },
            {
                timeout: 3 * SECOND,
                timeoutMsg: 'Саджест "Куда" на странице отеля не заполнился',
            },
        );

        const cityHotels =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

        assert.equal(
            arrivalCityAvia,
            cityHotels,
            'Город в отелях должен совпадать с городом прибытия в авиа',
        );

        if (hotelsApp.indexPage.isDesktop) {
            await this.browser.waitUntil(
                () => {
                    return hotelsApp.indexPage.searchForm.period.calendar.isDisplayed();
                },
                {
                    timeout: 5 * SECOND,
                    timeoutMsg: 'Календарь должен быть раскрыт',
                },
            );
        }
    });

    it('Сохранение контекста с выдачи на главную - жд-отели', async function () {
        const testApp = new TestApp(this.browser);
        const {hotelsApp, trainsApp} = testApp;

        await trainsApp.goToIndexPage();

        await trainsApp.indexPage.searchForm.fill({
            from: 'Москва',
            to: 'Санкт-Петербург',
            when: moment().add(3, 'day').format(dateFormats.ROBOT),
        });

        await trainsApp.indexPage.searchForm.submitForm();
        await trainsApp.searchPage.waitVariantsLoaded();

        if (trainsApp.searchPage.isTouch) {
            await trainsApp.searchPage.portalHeader.openSearchForm();
        }

        await trainsApp.searchPage.searchForm.fill({
            from: 'Казань',
            to: 'Москва',
            when: moment().add(5, 'day').format(dateFormats.ROBOT),
        });

        const arrivalCityTrains =
            await trainsApp.indexPage.searchForm.toSuggest.getInputValue();

        if (trainsApp.searchPage.isTouch) {
            await trainsApp.searchPage.searchForm.testModal.closeButton.click();
            await trainsApp.searchPage.header.navigationSideSheet.toggleButton.click();
            await trainsApp.searchPage.header.navigationSideSheet.hotelsLink.click();
        } else {
            await trainsApp.searchPage.header.navigations.hotels.click();
        }

        const cityHotels =
            await hotelsApp.indexPage.searchForm.place.getInputValue();

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

    it('Сохранение контекста с выдачи на главную - отели-авиа', async function () {
        const testApp = new TestApp(this.browser);
        const {aviaApp, hotelsApp} = testApp;

        await hotelsApp.goToIndexPage();

        await hotelsApp.indexPage.searchForm.fill({
            checkinDate: moment().add(3, 'day').format(dateFormats.ROBOT),
            checkoutDate: moment().add(6, 'day').format(dateFormats.ROBOT),
            place: 'Казань',
        });

        await hotelsApp.indexPage.searchForm.submitForm();

        if (hotelsApp.searchPage.isTouch) {
            await hotelsApp.searchPage.hotelsHeaderSearchInformation.openSearchForm();
        }

        await hotelsApp.searchPage.searchForm.fill({
            checkinDate: moment().add(5, 'day').format(dateFormats.ROBOT),
            checkoutDate: moment().add(8, 'day').format(dateFormats.ROBOT),
            place: 'Сочи',
        });

        const cityHotels =
            await hotelsApp.searchPage.searchForm.place.getInputValue();

        if (hotelsApp.searchPage.isTouch) {
            await hotelsApp.searchPage.searchForm.testModal.closeButton.click();
            await hotelsApp.searchPage.header.navigationSideSheet.toggleButton.click();
            await hotelsApp.searchPage.header.navigationSideSheet.aviaLink.click();
        } else {
            await hotelsApp.searchPage.header.navigations.avia.click();
        }

        const arrivalCityAvia =
            await aviaApp.indexPage.searchForm.toSuggest.getInputValue();

        assert.equal(
            arrivalCityAvia,
            cityHotels,
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
