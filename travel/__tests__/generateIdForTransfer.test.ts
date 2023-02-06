import ITransferFromBackend from '../../../../interfaces/transfer/ITransferFromBackend';
import {TransportType} from '../../../transportType';

import generateIdForTransfer from '../generateIdForTransfer';

describe('generateIdForTransfer', () => {
    it('Должен вернуться правильный id', () => {
        expect(
            generateIdForTransfer({
                segments: [
                    {
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
                    },
                    {
                        departure: '2019-08-17T04:44:00+00:00',
                        transport: {
                            code: TransportType.train,
                        },
                        stationFrom: {
                            id: 9606620,
                        },
                        stationTo: {
                            id: 9606503,
                        },
                    },
                ],
            } as ITransferFromBackend),
        ).toBe(
            '2000003-9606620-2019-08-16T12:30-train_9606620-9606503-2019-08-17T04:44-train',
        );
    });
});
