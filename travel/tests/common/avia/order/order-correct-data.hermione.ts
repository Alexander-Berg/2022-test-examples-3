import {assert} from 'chai';
import moment, {Moment} from 'moment';
import {order} from 'suites/avia';
import {random} from 'lodash';

import {MINUTE} from 'helpers/constants/dates';

import {AviaOrderPage} from 'helpers/project/avia/pages';
import {IAviaSearchFormParams} from 'helpers/project/avia/components/AviaSearchForm';
import {AviaOrderFlight} from 'helpers/project/avia/pages/OrderPage/components/Flight';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {TestAviaResultVariant} from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/TestAviaResultVariant';
import dateFormats from 'helpers/utilities/date/formats';

function getRandomDates(withReturnDate: boolean): Pick<
    IAviaSearchFormParams,
    'when' | 'return_date'
> & {
    whenMoment: Moment;
    returnDateMoment: Moment;
} {
    const whenMoment = moment().add(random(5, 10), 'day');
    const returnDateMoment = whenMoment.clone().add(3, 'day');

    return {
        whenMoment,
        when: whenMoment.format(dateFormats.ROBOT),
        returnDateMoment,
        return_date: withReturnDate
            ? returnDateMoment.format(dateFormats.ROBOT)
            : undefined,
    };
}

interface ITestParams {
    search: IAviaSearchFormParams & {
        whenMoment: Moment;
        returnDateMoment: Moment;
    };
    asserts: {
        titleForward: string;
        titleBackward?: string;
        klass: string;
        passengers: string;
    };
    variantCondition?: (variant: TestAviaResultVariant) => Promise<boolean>;
}

const searches: ITestParams[] = [
    {
        get search(): ITestParams['search'] {
            return {
                fromName: 'Москва',
                toName: 'Севастополь',
                ...getRandomDates(false),
            };
        },
        asserts: {
            titleForward: 'Москва — Симферополь',
            klass: 'эконом-класс',
            passengers: '1 пассажир',
        },
        /**
         * Находим вылет из любого пункта кроме Жуковского
         * @link https://st.yandex-team.ru/TRAVELFRONT-5840
         */
        variantCondition: async (variant): Promise<boolean> => {
            const forwardFlightInfo = await variant.getForwardFlightInfo();

            return !/ZIA/.test(forwardFlightInfo.departure);
        },
    },
    {
        get search(): ITestParams['search'] {
            return {
                fromName: 'Домодедово DME',
                toName: 'Пулково',
                ...getRandomDates(false),
            };
        },
        asserts: {
            titleForward: 'Москва — Санкт-Петербург',
            klass: 'эконом-класс',
            passengers: '1 пассажир',
        },
    },
    {
        get search(): ITestParams['search'] {
            return {
                fromName: 'Берлин Германия',
                toName: 'Париж Франция',
                klass: 'business',
                ...getRandomDates(false),
            };
        },
        asserts: {
            titleForward: 'Берлин — Париж',
            klass: 'бизнес-класс',
            passengers: '1 пассажир',
        },
    },
    {
        get search(): ITestParams['search'] {
            return {
                adult_seats: '2',
                fromName: 'Новосибирск',
                toName: 'Иркутск',
                ...getRandomDates(true),
            };
        },
        asserts: {
            titleForward: 'Новосибирск — Иркутск',
            titleBackward: 'Иркутск — Новосибирск',
            klass: 'эконом-класс',
            passengers: '2 пассажира',
        },
    },
];

describe(order.name, () => {
    searches.forEach(testCase => {
        hermione.config.testTimeout(2 * MINUTE);
        it(`Соответствие данных на покупке ${testCase.asserts.titleForward}`, async function () {
            const {search, asserts, variantCondition} = testCase;
            const {whenMoment, returnDateMoment} = search;
            const app = new TestAviaApp(this.browser);

            await app.goToIndexPage();
            await app.indexPage.search(search);

            await app.searchPage.waitForSearchComplete();

            const isTwoWay = Boolean(search.return_date);

            const variant = variantCondition
                ? await app.searchPage.variants.find(variantCondition)
                : await app.searchPage.variants.first();

            if (!variant) {
                throw new Error('Не найден вариант для тестового кейса');
            }

            const forwardVariantFlightInfo =
                await variant.getForwardFlightInfo();
            const backwardVariantFlightInfo = isTwoWay
                ? await variant.getBackwardFlightInfo()
                : null;
            const searchPrice = await variant.price.getText();

            await variant.moveToOrder();

            const orderPage = new AviaOrderPage(this.browser);

            await orderPage.waitForLoading();

            // context

            const orderPassengersTitle =
                await orderPage.offers.title.passengers.getText();
            const orderKlassTitle =
                await orderPage.offers.title.klass.getText();

            assert.equal(
                orderPassengersTitle,
                asserts.passengers,
                'неверный заголовок с количеством пассажиров',
            );

            assert.equal(
                orderKlassTitle,
                asserts.klass,
                'неверный заголовок с описанием класса перелёта',
            );

            // forward

            const forwardFlights: AviaOrderFlight[] = await orderPage.forward
                .flights.items;
            const forwardPointsTitle =
                await orderPage.forward.title.points.getText();
            const forwardDatesTitle =
                await orderPage.forward.title.dates.getText();
            const orderForwardDepartureTime =
                await forwardFlights[0].timings.departure.getText();
            const orderForwardArrivalTime = await forwardFlights[
                forwardFlights.length - 1
            ].timings.arrival.getText();

            assert.equal(
                asserts.titleForward,
                forwardPointsTitle,
                'заголовок "туда" не соответствует контексту поиска',
            );

            // не делаем более детального разбора т.к. заголовок с датами
            // может иметь различный вид в зависимости от дат отправления / прибытия
            assert(
                forwardDatesTitle?.startsWith(String(whenMoment.date())),
                `
                неверное отображение даты "туда"
                ожидание: тескт даты начинается с ${whenMoment.date()}
                реальность: ${forwardDatesTitle}
                `,
            );

            assert.equal(
                forwardVariantFlightInfo.departureTime,
                orderForwardDepartureTime,
                'время отправления "туда" на странице поиска и покупки различается',
            );

            assert.equal(
                forwardVariantFlightInfo.arrivalTime,
                orderForwardArrivalTime,
                'время прибытия "туда" на странице поиска и покупки различается',
            );

            // backward

            if (isTwoWay) {
                const backwardFlights: AviaOrderFlight[] = await orderPage
                    .backward.flights.items;
                const backwardPointsTitle =
                    await orderPage.backward.title.points.getText();
                const backwardDatesTitle =
                    await orderPage.backward.title.dates.getText();
                const orderBackwardDepartureTime =
                    await backwardFlights[0].timings.departure.getText();
                const orderBackwardArrivalTime = await backwardFlights[
                    backwardFlights.length - 1
                ].timings.arrival.getText();

                assert.equal(
                    asserts.titleBackward,
                    backwardPointsTitle,
                    'заголовок "обратно" не соответствует контексту поиска',
                );

                assert(
                    backwardDatesTitle?.startsWith(
                        String(returnDateMoment.date()),
                    ),
                    `
                    неверное отображение даты "туда"
                    ожидание: тескт даты начинается с ${returnDateMoment.date()}
                    реальность: ${backwardDatesTitle}
                    `,
                );

                assert.equal(
                    backwardVariantFlightInfo?.departureTime,
                    orderBackwardDepartureTime,
                    'время отправления "обратно" на странице поиска и покупки различается',
                );

                assert.equal(
                    backwardVariantFlightInfo?.arrivalTime,
                    orderBackwardArrivalTime,
                    'время прибытия "обратно" на странице поиска и покупки различается',
                );
            }

            // price

            const orderPrice = await (
                await orderPage.offers.cheapest
            ).price.getText();

            assert.equal(
                searchPrice,
                orderPrice,
                'цена на выдаче и на покупке различается',
            );
        });
    });
});
