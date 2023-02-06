import {assert} from 'chai';
import {random} from 'lodash';
import {passengers} from 'suites/account';
import moment from 'moment';

import {AVIA_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';
import {MINUTE} from 'helpers/constants/dates';

import PassengerLatinData from 'helpers/project/account/pages/PassengersPage/data/addPassengerLatin';
import {
    ADULT_MAN_WITH_PASSPORT,
    EAviaPassengerType,
    getPassengerDescription,
} from 'helpers/project/avia/pages/CreateOrderPage/passengers';
import PassengersPage from 'helpers/project/account/pages/PassengersPage/PassengersPage';
import Account from 'helpers/project/common/passport/Account';
import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {phoneNumber} from 'helpers/project/common/phoneNumber';

const {url: passengerPageUrl} = passengers;

describe('Авиабилеты: Бронирование', () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Бронирование авиабилетов неавторизованным пользователем', async function () {
        const app = new TestAviaApp(
            this.browser,
            AVIA_SUCCESS_TEST_CONTEXT_PARAMS,
        );

        const {happyPage, accountOrderPage} = app;

        const date = moment().add(1, 'month').add(random(1, 10), 'day');

        const {
            flights: [bookingFlight],
            price: bookingPrice,
        } = await app.book(
            {
                from: {name: 'Москва', id: 'c213'},
                to: {name: 'Сочи', id: 'c239'},
                startDate: date.format(dateFormats.ROBOT),
                travellers: {
                    adults: 1,
                    children: 0,
                    infants: 0,
                },
                klass: 'economy',
                filters: 'pt=aeroflot&c=0,26',
            },
            {
                passengers: [ADULT_MAN_WITH_PASSPORT],
                contacts: {
                    phone: phoneNumber,
                    email: 'test@test.ru',
                },
            },
        );

        await happyPage.waitForPageLoading();
        await happyPage.forwardToDetailedPage();
        await accountOrderPage.waitForPageLoading();

        assert.isNotEmpty(
            await accountOrderPage.pnr.number.getText(),
            'Должен отображаться номер для регистрации на рейс',
        );

        const {
            flight: [orderFlight],
            passengers: [orderPassenger],
        } = await accountOrderPage.getOrderData();

        assert.deepEqual(
            bookingFlight,
            orderFlight,
            `
                Информация о рейсе должна совпадать на странице бронирования и заказа.
                Бронирование: ${JSON.stringify(bookingFlight, null, 4)}
                Заказ: ${JSON.stringify(orderFlight, null, 4)}
            `,
        );

        assert.equal(
            getPassengerDescription(ADULT_MAN_WITH_PASSPORT),
            [
                orderPassenger.name,
                orderPassenger.document,
                orderPassenger.birthDate,
            ].join(' '),
            'Должны совпадать данные пассажира на странице бронирования и заказа',
        );

        const orderPrice = await accountOrderPage.getOrderPrice();

        assert.equal(
            bookingPrice,
            orderPrice,
            'Должна совпадать цена на странице бронирования и заказа',
        );

        assert.equal(
            await accountOrderPage.contacts.email.getText(),
            'test@test.ru',
            'Должен совпадать email на странице бронирования и заказа',
        );

        assert.equal(
            (await accountOrderPage.contacts.phone.getText()).replace(
                /[\s-]/g,
                '',
            ),
            phoneNumber,
            'Должен совпадать телефон на странице бронирования и заказа',
        );
    });

    hermione.config.testTimeout(4 * MINUTE);
    it('Бронирование авиабилетов авторизованным пользователем', async function () {
        const accountManager = new Account();
        const {
            account: {login, password},
        } = await accountManager.getOrCreate();

        const passengerPage = new PassengersPage(this.browser);

        await this.browser.login(login, password);
        await this.browser.url(passengerPageUrl);
        await passengerPage.removeAllPassengers();
        await passengerPage.addPassenger(PassengerLatinData, true);

        const app = new TestAviaApp(
            this.browser,
            AVIA_SUCCESS_TEST_CONTEXT_PARAMS,
        );

        const {createOrderPage, happyPage, accountOrderPage} = app;

        const date = moment().add(1, 'month').add(random(1, 10), 'day');

        const searchPage = await app.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Сочи', id: 'c239'},
            startDate: date.format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'pt=aeroflot&c=0,26',
        });

        await app.moveToBooking(searchPage);

        await createOrderPage.waitPageReadyForInteraction();

        const bookingPrice = await createOrderPage.priceInfo.getPrice();
        const [bookingFlight] = await createOrderPage.flightInfo.map(flight =>
            flight.getFlightData(),
        );

        await createOrderPage.form.fillPassengersFormWithIntent([
            {type: EAviaPassengerType.adult},
        ]);

        await createOrderPage.form.fillContactsForm({
            phone: phoneNumber,
            email: 'test@test.ru',
        });

        await createOrderPage.goToPayment();
        await happyPage.waitForPageLoading();
        await happyPage.forwardToDetailedPage();

        await accountOrderPage.waitForPageLoading();

        assert.isNotEmpty(
            await accountOrderPage.pnr.number.getText(),
            'Должен отображаться номер для регистрации на рейс',
        );

        const {
            flight: [orderFlight],
            passengers: [orderPassenger],
        } = await accountOrderPage.getOrderData();

        assert.deepEqual(
            bookingFlight,
            orderFlight,
            `
                Информация о рейсе должна совпадать на странице бронирования и заказа.
                Бронирование: ${JSON.stringify(bookingFlight, null, 4)}
                Заказ: ${JSON.stringify(orderFlight, null, 4)}
            `,
        );

        assert.equal(
            [
                orderPassenger.name,
                orderPassenger.document,
                orderPassenger.birthDate,
            ].join(' '),
            'Ivanov Ivan Ivanovich Паспорт РФ 6505123456 01.01.1970',
            'Должны совпадать данные пассажира на странице бронирования и заказа',
        );

        const orderPrice = await accountOrderPage.getOrderPrice();

        assert.equal(
            bookingPrice,
            orderPrice,
            'Должна совпадать цена на странице бронирования и заказа',
        );

        assert.equal(
            await accountOrderPage.contacts.email.getText(),
            'test@test.ru',
            'Должен совпадать email на странице бронирования и заказа',
        );

        assert.equal(
            (await accountOrderPage.contacts.phone.getText()).replace(
                /[\s-]/g,
                '',
            ),
            phoneNumber,
            'Должен совпадать телефон на странице бронирования и заказа',
        );

        // Удаляем пассажира
        await this.browser.url(passengerPageUrl);

        await passengerPage.removeAllPassengers();
    });
});
