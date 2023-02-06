import {DAYS, HOURS} from '../constants';
import {EDurationShortening} from '../../../interfaces/date/EDurationShortening';

jest.mock('../../../i18n/time', () =>
    jest.fn((key, {count}) => `${count} ${key}`),
);

const {formatDuration, humanizeDuration} = require.requireActual('../formats');
const moment = require.requireActual('moment');

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

        expect(formatDuration(hours3)).toBe('3 hours');
    });

    it('should format a duration with multiple consequent units using a short format', () => {
        const days2hours10minutes30 = moment.duration({
            days: 2,
            hours: 10,
            minutes: 30,
        });

        expect(formatDuration(days2hours10minutes30)).toBe(
            '2 days-short 10 hours-short 30 minutes-short',
        );
    });

    it('should format a duration omitting zero units in between using short format', () => {
        const weeks2minutes3 = moment.duration({weeks: 2, minutes: 3});

        expect(formatDuration(weeks2minutes3)).toBe(
            '2 weeks-short 3 minutes-short',
        );
    });

    it('should use a full format with short: "never" option', () => {
        const hours2minutes23 = moment.duration({hours: 2, minutes: 23});

        expect(
            formatDuration(hours2minutes23, {short: EDurationShortening.NEVER}),
        ).toBe('2 hours 23 minutes');
    });

    it('should use a short format with short: "always" option', () => {
        const days1 = moment.duration({days: 1});

        expect(formatDuration(days1, {short: EDurationShortening.ALWAYS})).toBe(
            '1 days-short',
        );
    });

    it('should accept `shortestUnit` option', () => {
        const hours1minutes10 = moment.duration({hours: 1, minutes: 10});

        expect(formatDuration(hours1minutes10, {shortestUnit: HOURS})).toBe(
            '1 hours',
        );
    });

    it('should accept `longestUnit` option', () => {
        const weeks1 = moment.duration({weeks: 1});

        expect(formatDuration(weeks1, {longestUnit: DAYS})).toBe('7 days');
    });
});

describe('humanizeDuration', () => {
    it('should round duration and then format it', () => {
        const duration = moment.duration({days: 2, minutes: 31});

        expect(humanizeDuration(duration)).toBe('2 days-short 1 hours-short');
    });
});
