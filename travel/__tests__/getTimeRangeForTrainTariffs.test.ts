import ISegment from '../../../../interfaces/segment/ISegment';
import ITransfer from '../../../../interfaces/transfer/ITransfer';
import {TransportType} from '../../../transportType';

import getTimeRangeForTrainTariffs from '../getTimeRangeForTrainTariffs';

function getSegment(
    transportType: TransportType,
    departure: string,
    hasTrainTariffs = false,
): ISegment {
    return {
        transport: {code: transportType},
        departure,
        hasTrainTariffs,
    } as ISegment;
}

function getTransfer(
    transportType: TransportType,
    departure: string,
): ITransfer {
    return {
        transport: {code: transportType},
        departure,
        isTransfer: true,
    } as ITransfer;
}

describe('getTimeRangeForTrainTariffs', () => {
    it('Вернет временной интервал по сегментам поезов', () => {
        const segments = [
            getSegment(TransportType.suburban, '2019-08-07T00:00:00+00:00'),
            getSegment(TransportType.train, '2019-08-08T00:00:00+00:00'),
            getSegment(TransportType.bus, '2019-08-10T00:00:00+00:00'),
            getSegment(TransportType.train, '2019-08-11T00:00:00+00:00'),
            getSegment(TransportType.plane, '2019-08-09T00:00:00+00:00'),
        ];

        expect(getTimeRangeForTrainTariffs(segments)).toEqual({
            startTime: '2019-08-08T00:00:00+00:00',
            endTime: '2019-08-11T00:01:00Z',
        });
    });

    it(`В рассчет интервала времени не берутся пересадки, потому что для них предусмотрен
    другой процесс для получения цен`, () => {
        const segments = [
            getSegment(TransportType.train, '2019-08-20T00:00:00+00:00'),
            getTransfer(TransportType.train, '2019-08-30T00:00:00+00:00'),
            getSegment(TransportType.train, '2019-08-10T00:00:00+00:00'),
        ];

        expect(getTimeRangeForTrainTariffs(segments)).toEqual({
            startTime: '2019-08-10T00:00:00+00:00',
            endTime: '2019-08-20T00:01:00Z',
        });
    });

    it('В случае если не нашлось подходящих сегментов вернется null', () => {
        const segments = [
            getTransfer(TransportType.suburban, '2019-08-30T00:00:00+00:00'),
            getSegment(TransportType.bus, '2019-08-30T00:00:00+00:00'),
        ];

        expect(getTimeRangeForTrainTariffs(segments)).toBe(null);
    });

    it('Включит в интервал для поездов электрички с поездатыми тарифами', () => {
        const segments = [
            getSegment(TransportType.train, '2019-08-20T00:00:00+00:00'),
            getSegment(
                TransportType.suburban,
                '2019-08-30T00:00:00+00:00',
                true,
            ),
            getSegment(TransportType.train, '2019-08-10T00:00:00+00:00'),
        ];

        expect(getTimeRangeForTrainTariffs(segments)).toEqual({
            startTime: '2019-08-10T00:00:00+00:00',
            endTime: '2019-08-30T00:01:00Z',
        });
    });
});
