import {times, constant} from 'lodash';

import {HOUR, MINUTE} from 'utilities/dateUtils/constants';

import {getFastestFlights} from 'selectors/avia/utils/getFastestFlights';
import {getGroupVariant} from 'selectors/avia/utils/__mocks__/mocks';

import {PriceComparator} from 'utilities/currency/compare';
import {PriceConverter} from 'utilities/currency/priceConverter';

const getFlight = (duration: number) =>
    getGroupVariant({
        forwardNumber: 'SU 342',
        duration,
    });

const priceComparator = new PriceComparator(new PriceConverter());

describe('getFastestFlights', () => {
    it('нет данных - вернёт пустой массив', () => {
        expect(getFastestFlights([], priceComparator)).toEqual([]);
    });

    it('время в пути одинаковое - вернёт исходный массив', () => {
        const flights = times(3, constant(getFlight(3 * HOUR)));
        const fastestFlights = getFastestFlights(flights, priceComparator);

        expect(flights.length).toBe(fastestFlights.length);
        expect(flights).toEqual(expect.arrayContaining(fastestFlights));
    });

    it('время в пути отличается, но не более относительной дельты - вернёт исходный массив', () => {
        const flights = [
            getFlight(5 * HOUR),
            getFlight(6 * HOUR - MINUTE),
            getFlight(5 * HOUR + MINUTE),
        ];
        const fastestFlights = getFastestFlights(flights, priceComparator);

        expect(fastestFlights.length).toBe(flights.length);
        expect(flights).toEqual(expect.arrayContaining(fastestFlights));
    });

    it('время в пути отличается, но не более 15 минут - вернёт исходный массив', () => {
        const flights = [
            getFlight(HOUR),
            getFlight(HOUR + 14 * MINUTE),
            getFlight(HOUR + MINUTE),
        ];
        const fastestFlights = getFastestFlights(flights, priceComparator);

        expect(fastestFlights.length).toBe(flights.length);
        expect(flights).toEqual(expect.arrayContaining(fastestFlights));
    });

    it('время в пути отличается (больше относительной дельты) - вернёт отфильтрованный массив', () => {
        const slowestFlight = getFlight(6 * HOUR + MINUTE);
        const flights = [
            getFlight(5 * HOUR),
            slowestFlight,
            getFlight(5 * HOUR + MINUTE),
        ];
        const fastestFlights = getFastestFlights(flights, priceComparator);

        expect(fastestFlights.length).toBe(2);
        expect(fastestFlights).not.toContain(slowestFlight);
    });

    it('время в пути отличается (больше 15 минут) - вернёт отфильтрованный массив', () => {
        const slowestFlight = getFlight(HOUR + 16 * MINUTE);
        const flights = [
            getFlight(HOUR),
            slowestFlight,
            getFlight(HOUR + MINUTE),
        ];
        const fastestFlights = getFastestFlights(flights, priceComparator);

        expect(fastestFlights.length).toBe(2);
        expect(fastestFlights).not.toContain(slowestFlight);
    });
});
