import getTransportTypes from '../getTransportTypes';
import {TRAIN_TYPE, BUS_TYPE, PLANE_TYPE} from '../../transportType';

describe('getTransportTypes', () => {
    it('should return empty array for empty array of segments', () => {
        expect(getTransportTypes([])).toEqual([]);
    });

    it('should return array with single item for bus segments', () => {
        const segments = Array.from({length: 10}, () => ({
            transport: {
                code: BUS_TYPE,
            },
        }));

        expect(getTransportTypes(segments)).toEqual([BUS_TYPE]);
    });

    it('should return array with all transport types', () => {
        const types = [BUS_TYPE, TRAIN_TYPE, PLANE_TYPE];
        const segments = Array.from({length: 10}, (item, index) => ({
            transport: {
                code: types[index % types.length],
            },
        }));

        expect(getTransportTypes(segments)).toEqual(types);
    });

    it('should return array with all transport types for transfer segments', () => {
        const types = [BUS_TYPE, TRAIN_TYPE];
        const segments = [
            {
                isTransfer: true,
                segments: Array.from({length: 5}, (item, index) => ({
                    transport: {
                        code: types[index % types.length],
                    },
                })),
            },
        ];

        expect(getTransportTypes(segments)).toEqual(types);
    });
});
