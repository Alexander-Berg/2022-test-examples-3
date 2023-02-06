import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import {MINUTE} from 'helpers/constants/dates';

import {ITestFormDocument} from 'helpers/project/common/components/TestBookingPassengerForm/types';

import dateFormats from 'helpers/utilities/date/formats';
import {card} from 'helpers/project/common/TestTrustForm/card';
import {
    maleGuests,
    femaleGuests,
} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import TestApp from 'helpers/project/TestApp';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    const testOfferParams = getTestOfferParams({
        occupancy: '2',
        checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
        checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
        priceAmount: 1000,
    });

    const bookHotel = async function (
        browser: WebdriverIO.Browser,
        options: {withPaymentContext: boolean},
    ): Promise<void> {
        const {withPaymentContext} = options;
        const guests = [...maleGuests, ...femaleGuests];

        const app = new TestHotelsBookApp(browser);
        const {hotelsBookPage, hotelsPaymentPage, hotelsHappyPage} = app;

        await hotelsBookPage.goToPage(testOfferParams);

        if (withPaymentContext) {
            await app.paymentTestContextHelper.setPaymentTestContext();
        }

        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        const bookPageHotelName = await hotelsBookPage.hotelName.getText();
        const bookPageCheckinDate =
            await hotelsBookPage.bookSearchParams.getCheckinDate();
        const bookPageCheckoutDate =
            await hotelsBookPage.bookSearchParams.getCheckoutDate();
        const bookPageTotalPrice =
            await hotelsBookPage.priceInfo.totalPrice.getText();

        await hotelsBookPage.bookForm.fillForm(guests, contacts);
        await hotelsBookPage.bookForm.submit();
        await hotelsBookPage.waitUntilLoaded();

        if (!withPaymentContext) {
            await hotelsPaymentPage.pageLoader.waitUntilLoaded();
            await hotelsPaymentPage.pay(card);
        }

        await hotelsPaymentPage.loader.waitUntilLoaded(120000);

        await hotelsHappyPage.loader.waitUntilLoaded();

        const hotelName = await hotelsHappyPage.orderInfo.hotelName.getText();
        const orderCheckinDate =
            await hotelsHappyPage.orderInfo.searchParams.getCheckinDate();
        const orderCheckoutDate =
            await hotelsHappyPage.orderInfo.searchParams.getCheckoutDate();

        assert.equal(
            bookPageHotelName,
            hotelName,
            'Имя гостинницы на HappyPage должно совпадать с именем на странице бронирования',
        );
        assert.equal(
            bookPageCheckinDate,
            orderCheckinDate,
            'Дата заезда на HappyPage должна совпадать с датой заезда на странице бронирования',
        );
        assert.equal(
            bookPageCheckoutDate,
            orderCheckoutDate,
            'Дата выезда на HappyPage должна совпадать с датой выезда на странице бронирования',
        );

        await hotelsHappyPage.orderActions.detailsLink.click();

        const orderPage = new TestOrderHotels(browser);

        await orderPage.loader.waitUntilLoaded();

        const totalPrice =
            await orderPage.mainInfo.orderHotelsPrice.totalPrice.totalPrice.getText();

        assert.equal(
            bookPageTotalPrice,
            totalPrice,
            'Полная цена бронирования на странице заказа в ЛК должна совпадать с полной ценой на странице бронирования',
        );

        const isRightGuests = await orderPage.guests.guests.every(
            async (guest, index) => {
                const pageGuest = await guest.name.getText();
                const dataGuest = getGuestFullName(guests[index]);

                return dataGuest === pageGuest;
            },
        );

        assert(
            isRightGuests,
            'Гости указанные в бронировании должны совпадать с гостями на странице заказа в ЛК',
        );
    };

    hermione.config.testTimeout(7 * MINUTE);
    it('Бронирование отеля НЕзалогин', async function () {
        await bookHotel(this.browser, {withPaymentContext: false});
    });

    hermione.config.testTimeout(7 * MINUTE);
    it('Бронирование отеля залогин', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        await bookHotel(this.browser, {withPaymentContext: true});
    });
});

function getGuestFullName(guest: ITestFormDocument): string {
    return `${guest.firstName} ${guest.lastName}`;
}
