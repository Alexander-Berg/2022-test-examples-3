import {momentTimezone as moment} from '../../../../reexports';

import TimeOfDay from '../../../interfaces/date/TimeOfDay';

import * as utils from '../utils';

const timezone = 'Asia/Yekaterinburg';

moment.locale('ru');

describe('getTimeOfDate', () => {
    it('should return NIGHT (NUMBER)', () => {
        expect(utils.getTimeOfDay(0)).toBe(TimeOfDay.night);
    });

    it('should return NIGHT (STRING)', () => {
        expect(utils.getTimeOfDay('0')).toBe(TimeOfDay.night);
    });

    it('should return MORNING (NUMBER)', () => {
        expect(utils.getTimeOfDay(6)).toBe(TimeOfDay.morning);
    });

    it('should return MORNING (STRING)', () => {
        expect(utils.getTimeOfDay('6')).toBe(TimeOfDay.morning);
    });

    it('should return DAY (NUMBER)', () => {
        expect(utils.getTimeOfDay(12)).toBe(TimeOfDay.day);
    });

    it('should return DAY (STRING)', () => {
        expect(utils.getTimeOfDay('12')).toBe(TimeOfDay.day);
    });

    it('should return EVENING (NUMBER)', () => {
        expect(utils.getTimeOfDay(18)).toBe(TimeOfDay.evening);
    });

    it('should return EVENING (STRING)', () => {
        expect(utils.getTimeOfDay('18')).toBe(TimeOfDay.evening);
    });
});

describe('getTimesOfDay', () => {
    it('should return [morning]', () => {
        expect(utils.getTimesOfDay(7, 11)).toEqual([TimeOfDay.morning]);
    });

    it('should return [morning, day, evening, night]', () => {
        expect(utils.getTimesOfDay(11, 9)).toEqual([
            TimeOfDay.morning,
            TimeOfDay.day,
            TimeOfDay.evening,
            TimeOfDay.night,
        ]);
    });

    it('should return [morning, day, evening]', () => {
        expect(utils.getTimesOfDay(7, 20)).toEqual([
            TimeOfDay.morning,
            TimeOfDay.day,
            TimeOfDay.evening,
        ]);
    });

    it('should return [evening, night, morning]', () => {
        expect(utils.getTimesOfDay(22, 11)).toEqual([
            TimeOfDay.evening,
            TimeOfDay.night,
            TimeOfDay.morning,
        ]);
    });
});

describe('getToday', () => {
    it('utc string to yekaterinburg day', () => {
        expect(
            utils
                .getToday({
                    now: '2016-02-13T22:00:00Z',
                    timezone: 'Asia/Yekaterinburg',
                })
                .format(),
        ).toBe('2016-02-14T00:00:00+05:00');
    });

    it('yekaterinburg string to vladivostok day', () => {
        expect(
            utils
                .getToday({
                    now: '2016-02-13T22:00:00+05:00',
                    timezone: 'Asia/Vladivostok',
                })
                .format(),
        ).toBe('2016-02-14T00:00:00+10:00');
    });
});

describe('getLastYearPeriod', () => {
    it('should return 2015/2016 for 2016-02-13', () => {
        expect(
            utils.getLastYearPeriod({
                now: '2016-02-13T22:00:00Z',
                timezone: 'Asia/Yekaterinburg',
            }),
        ).toBe('2015/2016');
    });
});

describe('getClosestWeekday', () => {
    // В качестве входного параметра используем дату 2016-02-13 (суббота).
    const date = moment.tz('2016-02-13T00:00:00+05:00', timezone);

    it('today', () => {
        expect(utils.getClosestWeekday(5, date).format()).toBe(
            '2016-02-13T00:00:00+05:00',
        );
    });

    it('tomorrow', () => {
        expect(utils.getClosestWeekday(6, date).format()).toBe(
            '2016-02-14T00:00:00+05:00',
        );
    });

    it('yesterday', () => {
        expect(utils.getClosestWeekday(4, date).format()).toBe(
            '2016-02-19T00:00:00+05:00',
        );
    });
});

describe('getRange', () => {
    it('2016-02-13 => [2016-01-14, 2017-01-13]', () => {
        const range = utils.getRange(
            moment.tz('2016-03-30T00:00:00+05:00', timezone),
        );

        expect(range.start.format()).toBe('2016-03-01T00:00:00+05:00');
        expect(range.end.format()).toBe('2017-02-28T00:00:00+05:00');
    });

    it('2016-03-31 => [2016-03-01, 2017-02-28]', () => {
        const range = utils.getRange(
            moment.tz('2016-03-30T00:00:00+05:00', timezone),
        );

        expect(range.start.format()).toBe('2016-03-01T00:00:00+05:00');
        expect(range.end.format()).toBe('2017-02-28T00:00:00+05:00');
    });
});

describe('getRangeFromDate', () => {
    it('2021-02-13 => [2021-02-13, 2022-02-13]', () => {
        const range = utils.getRangeFromDate(
            moment.tz('2021-02-13T00:00:00+05:00', timezone),
        );

        expect(range.start.format()).toBe('2021-02-13T00:00:00+05:00');
        expect(range.end.format()).toBe('2022-02-13T00:00:00+05:00');
    });
});

describe('fitInRange', () => {
    it('before range start', () => {
        const range = {
            start: moment.tz('2016-02-01T00:00:00+05:00', timezone),
            end: moment.tz('2017-01-31T00:00:00+05:00', timezone),
        };

        const date = moment.tz('2016-01-31T00:00:00+05:00', timezone);

        expect(utils.fitInRange(date, range).format()).toBe(
            '2017-01-31T00:00:00+05:00',
        );
    });

    it('at range start', () => {
        const range = {
            start: moment.tz('2016-02-01T00:00:00+05:00', timezone),
            end: moment.tz('2017-01-31T00:00:00+05:00', timezone),
        };
        const date = moment.tz('2016-02-01T00:00:00+05:00', timezone);

        expect(utils.fitInRange(date, range).format()).toBe(
            '2016-02-01T00:00:00+05:00',
        );
    });

    it('inside of range', () => {
        const range = {
            start: moment.tz('2016-02-01T00:00:00+05:00', timezone),
            end: moment.tz('2017-01-31T00:00:00+05:00', timezone),
        };
        const date = moment.tz('2016-05-01T00:00:00+05:00', timezone);

        expect(utils.fitInRange(date, range).format()).toBe(
            '2016-05-01T00:00:00+05:00',
        );
    });

    it('at range end', () => {
        const range = {
            start: moment.tz('2016-02-01T00:00:00+05:00', timezone),
            end: moment.tz('2017-01-31T00:00:00+05:00', timezone),
        };
        const date = moment.tz('2017-01-31T00:00:00+05:00', timezone);

        expect(utils.fitInRange(date, range).format()).toBe(
            '2017-01-31T00:00:00+05:00',
        );
    });

    it('after range end', () => {
        const range = {
            start: moment.tz('2016-02-01T00:00:00+05:00', timezone),
            end: moment.tz('2017-01-31T00:00:00+05:00', timezone),
        };
        const date = moment.tz('2017-02-01T00:00:00+05:00', timezone);

        expect(utils.fitInRange(date, range).format()).toBe(
            '2016-02-01T00:00:00+05:00',
        );
    });
});

describe('isWeekend', () => {
    it('saturday', () => {
        expect(utils.isWeekend(5)).toBe(true);
    });

    it('sunday', () => {
        expect(utils.isWeekend(6)).toBe(true);
    });

    it('monday', () => {
        expect(utils.isWeekend(0)).toBe(false);
    });

    it('friday', () => {
        expect(utils.isWeekend(4)).toBe(false);
    });
});

describe('getParseParams', () => {
    it('should transfer time and language', () => {
        const time = '2016-02-16T12:50:00+05:00';
        const language = 'uk';
        const params = {time, language, anotherField: 'some another field'};

        expect(utils.getParseParams(params)).toEqual({time, language});
    });
});

describe('getNextDayFormatted', () => {
    it('29.02.2016', () => {
        const date = '2016-02-29';

        expect(utils.getNextDayFormatted(date, timezone)).toEqual('2016-03-01');
    });
});

describe('getRemainingTime', () => {
    const baseMoment = moment.tz('2017-09-09T09:00:00+05:00', timezone);

    it('first moment is after second moment', () => {
        const eventMoment = moment.tz('2017-09-09T08:00:00+05:00', timezone);

        expect(utils.getRemainingTime(baseMoment, eventMoment)).toEqual({
            hours: 0,
            minutes: 0,
        });

        const nearEventMoment = moment.tz(
            '2017-09-09T08:59:59+05:00',
            timezone,
        );

        expect(utils.getRemainingTime(baseMoment, nearEventMoment)).toEqual({
            hours: 0,
            minutes: 0,
        });
    });

    it('first moment is equal second moment', () => {
        const eventMoment = moment.tz('2017-09-09T09:00:00+05:00', timezone);

        expect(utils.getRemainingTime(baseMoment, eventMoment)).toEqual({
            hours: 0,
            minutes: 0,
        });
    });

    it('moments are equal, timezones are different', () => {
        const eventMoment = moment.tz(
            '2017-09-09T09:00:00+05:00',
            'Europe/Moscow',
        );

        expect(utils.getRemainingTime(baseMoment, eventMoment)).toEqual({
            hours: 0,
            minutes: 0,
        });

        const eventMomentWithoutOffset = moment.tz(
            '2017-09-09T09:00:00',
            'Europe/Moscow',
        );
        const baseMomentWithoutOffset = moment.tz(
            '2017-09-09T09:00:00',
            timezone,
        );

        expect(
            utils.getRemainingTime(
                baseMomentWithoutOffset,
                eventMomentWithoutOffset,
            ),
        ).toEqual({
            hours: 2,
            minutes: 0,
        });
    });

    it('moments and timezones are different, timestamps are equal', () => {
        const eventMomentWithoutOffset = moment.tz(
            '2017-09-09T09:00:00',
            'Europe/Moscow',
        );
        const baseMomentWithoutOffset = moment.tz(
            '2017-09-09T11:00:00',
            timezone,
        );

        expect(
            utils.getRemainingTime(
                baseMomentWithoutOffset,
                eventMomentWithoutOffset,
            ),
        ).toEqual({
            hours: 0,
            minutes: 0,
        });
    });

    it('first moment is before second moment', () => {
        const nearEventMoment = moment.tz(
            '2017-09-09T09:00:01+05:00',
            timezone,
        );

        expect(utils.getRemainingTime(baseMoment, nearEventMoment)).toEqual({
            hours: 0,
            minutes: 0,
        });

        const eventMoment = moment.tz('2017-09-09T10:11:59+05:00', timezone);

        expect(utils.getRemainingTime(baseMoment, eventMoment)).toEqual({
            hours: 1,
            minutes: 11,
        });

        const farEventMoment = moment.tz('2017-09-10T10:11:59+05:00', timezone);

        expect(utils.getRemainingTime(baseMoment, farEventMoment)).toEqual({
            hours: 25,
            minutes: 11,
        });
    });
});

describe('formatLocalTime', () => {
    it('should return empty string', () => {
        expect(utils.formatLocalTime('')).toEqual('');
    });

    it('should return string with time', () => {
        expect(utils.formatLocalTime('12:30:00')).toEqual('12:30');
        expect(utils.formatLocalTime('10:00:00+05:00')).toEqual('10:00');
    });
});

describe('getMomentDate', () => {
    it('Вернёт moment объект соответствующий заданному времени и таймзоне', () => {
        const momentDate = utils.getMomentDate(629924400000, 'Europe/Moscow');

        expect(moment.isMoment(momentDate)).toBe(true);
    });
});

describe('getStartOfDate', () => {
    it('вернёт дату со временем установленным на начало дня', () => {
        const momentDate = moment('2016-02-13T12:08:33');
        const startOfDay = utils.getStartOfDate(momentDate);

        expect(startOfDay.format('YYYY-MM-DD HH:mm:ss')).toBe(
            '2016-02-13 00:00:00',
        );
    });
});

describe('isSameISODateString', () => {
    it('возвращает true, когда даты одинаковые', () => {
        expect(
            utils.isSameISODateString(
                '2017-09-09T10:00:00+01:00',
                '2017-09-09T10:00:00+01:00',
            ),
        ).toBe(true);

        expect(
            utils.isSameISODateString(
                '2017-09-09T10:00:00+01:00',
                '2017-09-09T14:00:00+05:00',
            ),
        ).toBe(true);

        expect(
            utils.isSameISODateString(
                '2017-09-09T10:00:00+01:00',
                '2017-09-09T14:00:00+05:00',
            ),
        ).toBe(true);
    });

    it('использует нулевое смещение, когда смещение не указано', () => {
        expect(
            utils.isSameISODateString(
                '2017-09-09T10:00',
                '2017-09-09T15:00:00+05:00',
            ),
        ).toBe(true);
    });

    it('возвращает false, когда даты разные или строки не являются датами в формате ISO', () => {
        expect(
            utils.isSameISODateString(
                '2017-09-09T10:00:00+01:00',
                '2018-09-09T14:00:00+05:00',
            ),
        ).toBe(false);

        expect(
            utils.isSameISODateString(
                '2017-09-09T10:00',
                '2017-09-09T16:00:00+05:00',
            ),
        ).toBe(false);

        expect(utils.isSameISODateString('qwerty', 'asdfgh')).toBe(false);

        expect(
            utils.isSameISODateString(
                '2017-09-09T16:00:00+05:00',
                '2017 09 09T16:00:00+05:00',
            ),
        ).toBe(false);
    });
});
