import {TransportType} from '../../../transportType';
import ISegment from '../../../../interfaces/segment/ISegment';

import areTrainTariffsAvailableForSegment from '../areTrainTariffsAvailableForSegment';

const getSegment = (code: TransportType): ISegment =>
    ({
        transport: {
            code,
        },
    } as ISegment);

describe('areTrainTariffsAvailable', () => {
    [
        TransportType.bus,
        TransportType.plane,
        TransportType.water,
        TransportType.suburban,
    ].forEach(type =>
        it(`Для ${type} сегмента - вернёт false`, () => {
            expect(areTrainTariffsAvailableForSegment(getSegment(type))).toBe(
                false,
            );
        }),
    );

    it('Для поезда - вернёт true', () => {
        expect(
            areTrainTariffsAvailableForSegment(getSegment(TransportType.train)),
        ).toBe(true);
    });

    it('Для электрички с флагом hasTrainTariffs - вернёт true', () => {
        expect(
            areTrainTariffsAvailableForSegment({
                ...getSegment(TransportType.suburban),
                hasTrainTariffs: true,
            }),
        ).toBe(true);
    });
});
