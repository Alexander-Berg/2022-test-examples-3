import {times, constant} from 'lodash';

import {getFlightsWithMinChanges} from 'selectors/avia/utils/getFlightsWithMinChanges';
import {getGroupVariant} from 'selectors/avia/utils/__mocks__/mocks';

const getFlight = (forwardTransfers = 0, backwardTransfers?: number) =>
    getGroupVariant({
        forwardNumber: 'SU 342',
        forwardTransfers,
        backwardNumber: (backwardTransfers && 'US 243') || undefined,
        backwardTransfers,
    });

const DIRECT_FLIGHT = getFlight();
const FLIGHT_WITH_TRANSFER = getFlight(1, 0);
const FLIGHT_WITH_TWO_TRANSFERS = getFlight(0, 2);
const FLIGHT_WITH_THREE_TRANSFERS = getFlight(2, 3);

describe('getFlightsWithMinChanges', () => {
    it('нет данных - вернёт пустой массив', () => {
        expect(getFlightsWithMinChanges([])).toEqual([]);
    });

    it('все перелёты имеют одинаковое код-во пересадок - вернёт исходный список перелётов', () => {
        const flights = times(3, constant(FLIGHT_WITH_TRANSFER));
        const minTransfersFlights = getFlightsWithMinChanges(flights);

        expect(flights.length).toEqual(minTransfersFlights.length);
    });

    it('количество пересадок во всех перелётах отличается не больше чем на одну - вернёт исходный список перелётов', () => {
        const flights = [
            DIRECT_FLIGHT,
            FLIGHT_WITH_TRANSFER,
            FLIGHT_WITH_TRANSFER,
            DIRECT_FLIGHT,
        ];
        const minTransfersFlights = getFlightsWithMinChanges(flights);

        expect(flights.length).toEqual(minTransfersFlights.length);
    });

    it('количество пересадок разное (разница > 1) - вернёт отфильтрованный список перелётов', () => {
        const flights = [
            FLIGHT_WITH_THREE_TRANSFERS,
            FLIGHT_WITH_TRANSFER,
            DIRECT_FLIGHT,
            FLIGHT_WITH_TWO_TRANSFERS,
        ];
        const minTransfersFlights = getFlightsWithMinChanges(flights);

        expect(minTransfersFlights.length).toBe(2);
        expect(minTransfersFlights).toContain(DIRECT_FLIGHT);
        expect(minTransfersFlights).toContain(FLIGHT_WITH_TRANSFER);
    });
});
