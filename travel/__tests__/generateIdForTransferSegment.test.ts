import {TransportType} from '../../../transportType';
import ITransferSegmentFromBackend from '../../../../interfaces/transfer/ITransferSegmentFromBackend';

import generateIdForTransferSegment from '../generateIdForTransferSegment';

describe('generateIdForTransferSegment', () => {
    it('Вернет корректный id', () => {
        expect(
            generateIdForTransferSegment({
                departure: '2019-08-16T12:30:00+00:00',
                transport: {
                    code: TransportType.train,
                },
                stationFrom: {
                    id: 2000003,
                },
                stationTo: {
                    id: 9606620,
                },
            } as ITransferSegmentFromBackend),
        ).toBe('2000003-9606620-2019-08-16T12:30-train');
    });
});
