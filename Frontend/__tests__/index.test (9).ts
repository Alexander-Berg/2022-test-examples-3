import i18n from '../../../../shared/lib/i18n';
import * as ru from '../../../../langs/yamb/ru.json';
import * as en from '../../../../langs/yamb/en.json';

import { formatDuration, convertTimeToSeconds, getRelativeTime } from '..';
import { dateFull, dayAndMonth } from '../DateFormat';

describe('formatDuration', () => {
    it('should return correct string for value -10', () => {
        expect(formatDuration(-10)).toEqual('00:00');
    });

    it('should return correct string for value 0', () => {
        expect(formatDuration(0)).toEqual('00:00');
    });

    it('should return correct string for value 1', () => {
        expect(formatDuration(1)).toEqual('00:01');
    });

    it('should return correct string for value 10', () => {
        expect(formatDuration(10)).toEqual('00:10');
    });

    it('should return correct string for value 59', () => {
        expect(formatDuration(59)).toEqual('00:59');
    });

    it('should return correct string for value 60', () => {
        expect(formatDuration(60)).toEqual('01:00');
    });

    it('should return correct string for value 3610', () => {
        expect(formatDuration(3610)).toEqual('01:00:10');
    });
});

describe('dateFull', () => {
    const date = 1556197558307;

    it('returns correct value (ru)', () => {
        i18n.locale('ru', ru);

        const expected = '25 апреля 2019 г.';

        expect(dateFull(new Date(date))).toBe(expected);
    });

    it('returns correct value (en)', () => {
        i18n.locale('en', en);

        const expected = '25 April 2019';

        expect(dateFull(new Date(date))).toBe(expected);
    });
});

describe('dayAndMonth', () => {
    const date = 1556197558307;

    it('returns correct value (ru)', () => {
        i18n.locale('ru', ru);

        const expected = '25 апреля';

        expect(dayAndMonth(new Date(date))).toBe(expected);
    });

    it('returns correct value (en)', () => {
        i18n.locale('en', en);

        const expected = '25 April';

        expect(dayAndMonth(new Date(date))).toBe(expected);
    });
});

describe('convertTimeToSeconds', () => {
    it('convertTimeToSeconds returns integer seconds', () => {
        expect(convertTimeToSeconds('12:00')).toBe(12 * 60 * 60);
        expect(convertTimeToSeconds('00:00')).toBe(0);
        expect(convertTimeToSeconds('00:05')).toBe(5 * 60);
        expect(convertTimeToSeconds('24:00')).toBe(24 * 60 * 60);
    });
});

describe('getRelativeTime', () => {
    const todayDate = new Date(2022, 1, 9);

    it('getRelativeTime returns full date if last year', () => {
        expect(getRelativeTime(new Date(2021, 1, 1), todayDate)).toBe('1 February 2021');
        expect(getRelativeTime(new Date(2015, 0, 5), todayDate)).toBe('5 January 2015');
    });

    it('getRelativeTime returns day and month if time has left', () => {
        expect(getRelativeTime(new Date(2022, 1, 1), todayDate)).toBe('1 February');
        expect(getRelativeTime(new Date(2022, 0, 15), todayDate)).toBe('15 January');
    });

    it('getRelativeTime returns Yerstaday if date has been yesterday', () => {
        expect(getRelativeTime(new Date(2022, 1, 8), todayDate)).toBe('Yesterday');
    });

    it('getRelativeTime returns Today if date has been today', () => {
        expect(getRelativeTime(new Date(2022, 1, 9), todayDate)).toBe('Today');
    });

    it('getRelativeTime returns empty string if date has been today and droptoday', () => {
        expect(getRelativeTime(new Date(2022, 1, 9), todayDate, { droptoday: true })).toBe('');
    });

    it('getRelativeTime returns day and month if date in future and current year', () => {
        expect(getRelativeTime(new Date(2022, 2, 9), todayDate)).toBe('9 March');
    });

    it('getRelativeTime returns full date if date in future and future year', () => {
        expect(getRelativeTime(new Date(2023, 0, 8), todayDate)).toBe('8 January 2023');
    });
});
