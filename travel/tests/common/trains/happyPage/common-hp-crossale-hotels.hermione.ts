import {assert} from 'chai';
import {crossSale} from 'suites/komod';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';

describe(crossSale.name, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Карта отелей на хп жд', async function () {
        const app = new TestTrainsApp(this.browser);
        const testApp = new TestApp(this.browser);

        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            paymentPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();

        const startDate = await happyPage.orderMainInfo.getStartDate();

        /*        const momentDate = moment(getDate, 'DD-MM');*/

        const {crossSales} = happyPage;

        await crossSales.hotelsCrossSaleMap.scrollIntoView();
        await crossSales.hotelsCrossSaleMap.waitForLoading();

        assert.isTrue(
            await crossSales.hotelsCrossSaleMap.mapCard.crossSaleMap.map.isVisible(),
            'Должна отображаться кроссейл карта',
        );

        if (happyPage.isDesktop) {
            assert.isTrue(
                await crossSales.hotelsCrossSaleMap.mapCard.crossSaleMap.map.hasActiveHotelCard(),
                'Должна отображаться карточка отеля в кроссейле',
            );
        }

        await crossSales.hotelsCrossSaleMap.mapCard.crossSaleMap.click();

        await this.browser.switchToNextTab();

        const {hotelsApp} = testApp;

        const hotelsSearchPage = hotelsApp.searchPage;

        const hotelsSearchFormDates =
            await hotelsSearchPage.getSearchFormDates();

        assert.equal(
            hotelsSearchFormDates.startDate.format(dateFormats.HUMAN_SHORT),
            startDate.format(dateFormats.HUMAN_SHORT),
            'Дата заезда на выдаче отелей должна совпадать с датой отправления на хп',
        );

        const expectedHotelsEndDate = startDate.add(1, 'days');

        assert.equal(
            hotelsSearchFormDates.endDate.format(dateFormats.HUMAN_SHORT),
            expectedHotelsEndDate.format(dateFormats.HUMAN_SHORT),
            'Дата выезда на выдаче отелей должна быть на 1 день больше даты отправления',
        );
    });
});
