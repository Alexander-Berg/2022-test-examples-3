/*eslint-disable @typescript-eslint/no-magic-numbers*/

import { getChartClosePointIndex } from 'shared/helpers/getChartClosePointIndex/getChartClosePointIndex';
import { ChartPoint } from 'shared/types/ChartPoint';

const POINT_LIST = [
    { begin_date: 1598907600000, end_date: 1598994000000 },
    { begin_date: 1598994000000, end_date: 1599080400000 },
    { begin_date: 1599080400000, end_date: 1599166800000 },
    { begin_date: 1599166800000, end_date: 1599253200000 },
] as ChartPoint[];

describe('getChartClosePointIndex', function () {
    it('should find index of the closest point by date', function () {
        expect(getChartClosePointIndex(POINT_LIST, 1599080499999)).toBe(2);
    });

    it('should return -1 for empty points array', function () {
        expect(getChartClosePointIndex([], 1599080499999)).toBe(-1);
    });

    it('should return 0 index for single-item array', function () {
        expect(
            getChartClosePointIndex(
                [{ begin_date: 1598907600000, end_date: 1598994000000 } as ChartPoint],
                1599080499999,
            ),
        ).toBe(0);
    });
});
