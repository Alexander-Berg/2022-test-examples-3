import {withoutAirportChange} from 'selectors/avia/utils/withoutAirportChange';
import {getGroupVariant} from 'selectors/avia/utils/__mocks__/mocks';
import {IResultAviaFlight} from 'selectors/avia/utils/denormalization/flight';
import {IAviaVariantGroup} from 'selectors/avia/utils/denormalization/variantGroup';
import {IResultAviaStation} from 'selectors/avia/utils/denormalization/station';

const addStations = (flights: IResultAviaFlight[], stations: number[]) =>
    flights.map((flight, index) => ({
        ...flight,
        from: {
            id: stations[index * 2],
        } as IResultAviaStation,
        to: {
            id: stations[index * 2 + 1],
        } as IResultAviaStation,
    }));

const getFlight = (
    forwardStations: number[],
    backwardStations?: number[],
): IAviaVariantGroup => {
    const group = getGroupVariant({
        forwardNumber: 'SU 342',
        backwardNumber: 'US 243',
        forwardTransfers: forwardStations.length / 2 - 1,
        backwardTransfers: backwardStations
            ? backwardStations.length / 2 - 1
            : 0,
    });

    return {
        ...group,
        variants: group.variants.map(variant => ({
            ...variant,
            forward: addStations(variant.forward, forwardStations),
            backward:
                (backwardStations &&
                    addStations(variant.backward, backwardStations)) ||
                [],
        })),
    };
};

describe('filterByAirportChange', () => {
    it('нет смены аэропорта - вернём true', () => {
        expect(
            withoutAirportChange(
                getFlight([10, 55, 55, 69], [69, 55, 55, 10]),
                true,
            ),
        ).toBe(true);
    });

    it('есть смена аэропорта в обратном направлении, но мы не учитываем обратное направление - вернём true', () => {
        expect(
            withoutAirportChange(
                getFlight([10, 55, 55, 69], [69, 55, 55, 10]),
                false,
            ),
        ).toBe(true);
    });

    it('есть смена аэропорта в прямом направлении - вернём false', () => {
        expect(
            withoutAirportChange(
                getFlight([10, 55, 58, 69], [69, 55, 55, 10]),
                true,
            ),
        ).toBe(false);
    });

    it('есть смена аэропорта в обратном направлении - вернём false', () => {
        expect(
            withoutAirportChange(
                getFlight([10, 55, 55, 69], [69, 55, 58, 10]),
                true,
            ),
        ).toBe(false);
    });
});
