import {momentTimezone as moment} from '../../../../reexports';

import {
    updateSpecialTime,
    updateDateTime,
    updateWeekdayTime,
} from '../updateByType';
import {getToday, getRange, fitInRange, getClosestWeekday} from '../utils';

jest.mock('../utils');

const timezone = 'Asia/Yekaterinburg';

moment.locale('ru');

const time = {
    now: '2016-02-17T03:45:00+05:00',
    timezone,
};

const today = moment.tz('2016-02-17', timezone);

describe('updateByType', () => {
    beforeEach(() => {
        getToday.mockReturnValue(today);
    });

    describe('updateSpecialTime', () => {
        it('tomorrow', () => {
            const value = {
                foo: 'bar',
                special: 'tomorrow',
            };

            expect(updateSpecialTime(value, time)).toEqual({
                foo: 'bar',
                special: 'tomorrow',
                date: '2016-02-18',
            });

            expect(getToday).toBeCalledWith(time);
        });

        it('yesterday', () => {
            const value = {
                foo: 'bar',
                special: 'yesterday',
            };

            expect(updateSpecialTime(value, time)).toEqual({
                foo: 'bar',
                special: 'yesterday',
                date: '2016-02-16',
            });

            expect(getToday).toBeCalledWith(time);
        });

        it('unknown', () => {
            const value = {
                foo: 'bar',
                special: 'unknown',
            };

            expect(updateSpecialTime(value, time)).toEqual({
                foo: 'bar',
                special: 'unknown',
            });

            expect(getToday).not.toBeCalled();
        });
    });

    it('updateDateTime', () => {
        const value = {
            foo: 'bar',
            date: '2016-03-01',
        };

        const range = {
            start: moment.tz('2016-01-01', timezone),
            end: moment.tz('2016-12-31', timezone),
        };

        getRange.mockReturnValue(range);
        fitInRange.mockReturnValue(moment.tz('2016-03-01', timezone));

        expect(updateDateTime(value, time)).toEqual({
            foo: 'bar',
            date: '2016-03-01',
        });

        expect(getToday).toBeCalledWith(time);
        expect(getRange).toBeCalledWith(today);

        expect(fitInRange).toBeCalled();
        expect(fitInRange.mock.calls[0][0].format()).toBe(
            '2016-03-01T00:00:00+05:00',
        );
        expect(fitInRange.mock.calls[0][1]).toBe(range);
    });

    it('updateWeekdayTime', () => {
        const value = {
            foo: 'bar',
            weekday: 5,
        };

        const closestWeekday = moment.tz('2016-02-20', timezone);

        getClosestWeekday.mockReturnValue(closestWeekday);

        expect(updateWeekdayTime(value, time)).toEqual({
            foo: 'bar',
            weekday: 5,
            date: '2016-02-20',
        });

        expect(getToday).toBeCalledWith(time);
        expect(getClosestWeekday).toBeCalledWith(5, today);
    });
});
