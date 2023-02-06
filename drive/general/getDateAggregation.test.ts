import { ONE_MONTH, ONE_SECOND, ONE_WEEK } from 'constants/constants';

import { DateAggregation } from 'shared/consts/DateAggregation';
import { DateFilter } from 'shared/consts/DateFilter';
import { getDateAggregation } from 'shared/helpers/getDateAggregation/getDateAggregation';
import { getDateEndDay } from 'shared/helpers/getDateEndDay/getDateEndDay';
import { getDateStartDay } from 'shared/helpers/getDateStartDay/getDateStartDay';

describe('getDateAggregation', () => {
    it('works by default', () => {
        expect(getDateAggregation({}).aggregation).toEqual(DateAggregation.DAY);
    });

    it('works for days', () => {
        expect(
            getDateAggregation({
                [DateFilter.SINCE]: (getDateStartDay(new Date()).getTime() - ONE_WEEK * 2) / ONE_SECOND,
                [DateFilter.UNTIL]: Math.trunc(getDateEndDay(new Date()).getTime() / ONE_SECOND),
            }).aggregation,
        ).toEqual(DateAggregation.DAY);
    });

    it('works for weeks', () => {
        expect(
            getDateAggregation({
                // eslint-disable-next-line @typescript-eslint/no-magic-numbers
                [DateFilter.SINCE]: (getDateStartDay(new Date()).getTime() - ONE_MONTH * 3) / ONE_SECOND,
                [DateFilter.UNTIL]: Math.trunc(getDateEndDay(new Date()).getTime() / ONE_SECOND),
            }).aggregation,
        ).toEqual(DateAggregation.WEEK);
    });

    it('works for month', () => {
        expect(
            getDateAggregation({
                // eslint-disable-next-line @typescript-eslint/no-magic-numbers
                [DateFilter.SINCE]: (getDateStartDay(new Date()).getTime() - ONE_MONTH * 9) / ONE_SECOND,
                [DateFilter.UNTIL]: Math.trunc(getDateEndDay(new Date()).getTime() / ONE_SECOND),
            }).aggregation,
        ).toEqual(DateAggregation.MONTH);
    });
});
