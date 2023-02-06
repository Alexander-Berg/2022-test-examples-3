import { getChartArrayPoints } from 'shared/helpers/getChartArrayPoints/getChartArrayPoints';
import { ChartPoint } from 'shared/types/ChartPoint';

/*eslint-disable @typescript-eslint/no-magic-numbers*/

const POINT: ChartPoint = {
    begin_date: 0,
    end_date: 0,
    line1: 13.2,
    line2: 21,
    line3: 1260,
    line4: [10, 23],
    line5: [0, 0],
};

describe('getChartArrayPoints', function () {
    it('works with numbers', function () {
        expect(getChartArrayPoints(POINT, 'line1')).toEqual([13.2]);
    });

    it('works with array of numbers', function () {
        expect(getChartArrayPoints(POINT, 'line4')).toEqual([10, 23]);
    });
});
