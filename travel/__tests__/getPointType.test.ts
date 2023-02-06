import Point from '../../../interfaces/Point';
import PointType from '../../../interfaces/PointType';

import getPointType from '../getPointType';

describe('getPointType', () => {
    it('station', () => {
        expect(getPointType('s123' as Point)).toBe(PointType.station);
    });

    it('city', () => {
        expect(getPointType('c123' as Point)).toBe(PointType.city);
    });
});
