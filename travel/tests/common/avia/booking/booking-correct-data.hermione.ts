import {assert} from 'chai';
import moment from 'moment';
import {random} from 'lodash';

import {AVIA_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import extractNumber from 'helpers/utilities/extractNumber';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import dateFormats from 'helpers/utilities/date/formats';

interface IFLightData {
    departure: string;
    arrival: string;
    duration: string;
    fromCity: string;
    toCity: string;
    fromAirport: string;
    toAirport: string;
    flightNumber: string;
}

describe('Авиабилеты: Бронирование', function () {
    it('BOY Проверка совпадения данных на странице', async function () {
        const date = moment().add(1, 'month').add(random(1, 10), 'day');

        const app = new TestAviaApp(
            this.browser,
            AVIA_SUCCESS_TEST_CONTEXT_PARAMS,
        );
        const {searchPage, orderPage, createOrderPage} = app;

        await app.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Иркутск', id: 'c63'},
            startDate: date.format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'pt=aeroflot&c=0,26',
        });
        await searchPage.waitForSearchComplete();

        const searchVariant = await searchPage.variants.first();

        await searchVariant.moveToOrder();

        const companyOffer = await orderPage.offers.company;
        const orderPrice = await companyOffer.price.getText();
        const orderFlightsData: IFLightData[] =
            await orderPage.forward.flights.map(async flight => ({
                departure: await flight.timings.departure.getText(),
                arrival: await flight.timings.arrival.getText(),
                duration: await flight.timings.duration.getText(),
                fromCity: await flight.fromCity.getText(),
                toCity: await flight.toCity.getText(),
                fromAirport: await flight.fromAirport.getText(),
                toAirport: await flight.toAirport.getText(),
                flightNumber: await flight.planeNumber.getText(),
            }));

        await companyOffer.scrollIntoView();
        await companyOffer.click();

        // явным образом переключаем вкладку, т.к. без этой команды
        // вебдрайвер продолжит взаимодействоватьс неактивной вкладкой
        await this.browser.switchToNextTab();

        await createOrderPage.waitForPageLoading(45000);

        const isPriceChanged = await createOrderPage.modalError.isVisible(
            10000,
        );

        await createOrderPage.skipModals();

        const bookingFlightsData: IFLightData[] =
            await createOrderPage.flightInfo.map(async flight => ({
                departure: await flight.departure.getText(),
                arrival: await flight.arrival.getText(),
                duration: await flight.duration.getText(),
                fromCity: await flight.fromCity.getText(),
                toCity: await flight.toCity.getText(),
                fromAirport: await flight.fromAirport.getText(),
                toAirport: await flight.toAirport.getText(),
                flightNumber: await flight.flightNumber.getText(),
            }));

        const maxFlightsCount = Math.max(
            orderFlightsData.length,
            bookingFlightsData.length,
        );

        for (let i = 0; i < maxFlightsCount; i++) {
            const orderFlight = orderFlightsData[i];
            const bookingFlight = bookingFlightsData[i];

            Object.keys(orderFlight).forEach(key => {
                /**
                 * иата коррекция
                 * могут поменятся буквы в рейсе
                 * сравниваем только цифры
                 */
                if (key === 'flightNumber') {
                    assert.equal(
                        extractNumber(orderFlight.flightNumber),
                        extractNumber(bookingFlight.flightNumber),
                        `Различается значение flightNumber для ${i} перелёта`,
                    );

                    return;
                }

                assert.equal(
                    orderFlight[key as keyof IFLightData],
                    bookingFlight[key as keyof IFLightData],
                    `Различается значение ${key} для ${i} перелёта`,
                );
            });
        }

        // Проверяем совпадение цены только в том случае если мы не показывали окно
        // с уведомлением о изменении цены
        if (!isPriceChanged) {
            assert.equal(
                orderPrice,
                await createOrderPage.priceInfo.getPrice(),
                'Различается цена на покупке и на бронировании',
            );
        }
    });
});
