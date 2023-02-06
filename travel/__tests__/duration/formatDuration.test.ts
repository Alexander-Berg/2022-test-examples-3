import moment from 'moment';

import {
    EShorteningOption,
    ETimeUnit,
} from 'projects/trains/lib/date/duration/constants';

import formatDuration from 'projects/trains/lib/date/duration/formatDuration';

describe('formatDuration', () => {
    it('should return an empty string for an empty duration', () => {
        const zeroDuration = moment.duration();

        expect(formatDuration(zeroDuration)).toBe('');
    });

    it('should return an empty string for a durations shorter than a shortest unit', () => {
        const seconds59 = moment.duration(59, 'seconds');

        expect(formatDuration(seconds59)).toBe('');
    });

    it('should format a duration with one unit', () => {
        const hours3 = moment.duration({hours: 3});

        expect(formatDuration(hours3)).toBe('3 часа');
    });

    it('should format a duration with multiple consequent units using a short format', () => {
        const days2hours10minutes30 = moment.duration({
            days: 2,
            hours: 10,
            minutes: 30,
        });

        expect(formatDuration(days2hours10minutes30)).toBe('2 дн. 10 ч 30 мин');
    });

    it('should format a duration omitting zero units in between using short format', () => {
        const weeks2minutes3 = moment.duration({weeks: 2, minutes: 3});

        expect(formatDuration(weeks2minutes3)).toBe('2 нед. 3 мин');
    });

    it('should use a full format with short: "never" option', () => {
        const hours2minutes23 = moment.duration({hours: 2, minutes: 23});

        expect(
            formatDuration(hours2minutes23, {short: EShorteningOption.NEVER}),
        ).toBe('2 часа 23 минуты');
    });

    it('should use a short format with short: "always" option', () => {
        const days1 = moment.duration({days: 1});

        expect(formatDuration(days1, {short: EShorteningOption.ALWAYS})).toBe(
            '1 дн.',
        );
    });

    it('should accept `shortesUnit` option', () => {
        const hours1minutes10 = moment.duration({hours: 1, minutes: 10});

        expect(
            formatDuration(hours1minutes10, {shortestUnit: ETimeUnit.HOURS}),
        ).toBe('1 час');
    });

    it('should accept `longestUnit` option', () => {
        const weeks1 = moment.duration({weeks: 1});

        expect(formatDuration(weeks1, {longestUnit: ETimeUnit.DAYS})).toBe(
            '7 дней',
        );
    });
});
