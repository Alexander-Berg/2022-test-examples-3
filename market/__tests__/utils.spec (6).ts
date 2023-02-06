import {isDate} from 'utils/typeGuards';

import {getStartDateOfMonth} from '../utils';

const ZERO_TIME = '00:00:00';

describe('getStartDateOfMonth', () => {
    const date = new Date();
    const dtCurr = getStartDateOfMonth();
    const dtPrev = getStartDateOfMonth(date);
    const currMonth = date.getMonth();
    const currDtMonth = dtCurr.getMonth();
    const currDtDay = dtCurr.getDate();
    const currDtTime = dtCurr.toTimeString().split(' ')[0];
    const prevDtMonth = dtPrev.getMonth();
    const prevDtDay = dtPrev.getDate();
    const prevDtTime = dtPrev.toTimeString().split(' ')[0];

    it('called without arguments returns Date', () => {
        expect(isDate(dtCurr)).toBeTruthy();
    });

    it('called with (new Date()) returns Date', () => {
        expect(isDate(dtPrev)).toBeTruthy();
    });

    it('called without arguments returns current month', () => {
        expect(currDtMonth).toEqual(currMonth);
    });

    it('called with (new Date()) returns previous month', () => {
        const isPrevMonthOk =
            prevDtMonth !== currMonth && ((currMonth > 0 && prevDtMonth === currMonth - 1) || prevDtMonth === 11);

        expect(isPrevMonthOk).toBeTruthy();
    });

    it('called without arguments returns Date with day equal to 1', () => {
        expect(currDtDay).toEqual(1);
    });

    it('called with (new Date()) returns Date with day equal to 1', () => {
        expect(prevDtDay).toEqual(1);
    });

    it(`called without arguments returns Date with time equal to ${ZERO_TIME}`, () => {
        expect(currDtTime).toEqual(ZERO_TIME);
    });

    it(`called with (new Date()) returns Date with day equal to ${ZERO_TIME}`, () => {
        expect(prevDtTime).toEqual(ZERO_TIME);
    });
});
