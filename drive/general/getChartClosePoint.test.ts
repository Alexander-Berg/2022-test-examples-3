import { getChartClosePoint } from 'shared/helpers/getChartClosePoint/getChartClosePoint';
import { ChartPoint } from 'shared/types/ChartPoint';

/*eslint-disable @typescript-eslint/no-magic-numbers*/

const LEFT_POINT = {
    begin_date: 1599080400000,
    end_date: 1599166800000,
} as ChartPoint;

const RIGHT_POINT = {
    begin_date: 1599166800000,
    end_date: 1599253200000,
} as ChartPoint;

describe('getChartClosePoint', function () {
    it('should find closest point by date', function () {
        expect(getChartClosePoint([LEFT_POINT, RIGHT_POINT], 1599080499999)).toBe(LEFT_POINT);
    });

    it('should return undefined for empty point array', function () {
        expect(getChartClosePoint([], 1599080499999)).toBeUndefined();
    });

    it('should return single item in array', function () {
        expect(getChartClosePoint([RIGHT_POINT], 1599080499999)).toBe(RIGHT_POINT);
    });
});
