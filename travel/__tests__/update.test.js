import {updateTime} from '../update';
import {
    updateSpecialTime,
    updateWeekdayTime,
    updateDateTime,
} from '../updateByType';

jest.mock('../updateByType');

const timezone = 'Asia/Yekaterinburg';

const time = {
    now: '2016-02-17T03:45:00+05:00',
    timezone,
};

const expectedValue = {
    date: '2016-03-01',
};

describe('updateTime', () => {
    it('special', () => {
        const value = {
            special: 'tomorrow',
        };

        updateSpecialTime.mockReturnValue(expectedValue);

        expect(updateTime(value, time)).toBe(expectedValue);
        expect(updateSpecialTime).toBeCalledWith(value, time);
    });

    it('weekday', () => {
        const value = {
            weekday: 5,
        };

        updateWeekdayTime.mockReturnValue(expectedValue);

        expect(updateTime(value, time)).toBe(expectedValue);
        expect(updateWeekdayTime).toBeCalledWith(value, time);
    });

    it('date', () => {
        const value = {
            date: '2015-03-01',
        };

        updateDateTime.mockReturnValue(expectedValue);

        expect(updateTime(value, time)).toBe(expectedValue);
        expect(updateDateTime).toBeCalledWith(value, time);
    });

    it('default', () => {
        const value = {
            foo: 'bar',
        };

        expect(updateTime(value, time)).toBe(value);
    });
});
