const moment = require.requireActual('moment');

jest.dontMock('../constants');
const {roundDuration} = require.requireActual('../durations');

describe('roundDuration', () => {
    describe('default settings', () => {
        it('0 -> 0', () => {
            const duration = moment.duration();

            expect(roundDuration(duration).asMilliseconds()).toBe(0);
        });

        it('1w 6d 12h 0m 1s -> 2w', () => {
            const duration = moment.duration({
                weeks: 1,
                days: 6,
                hours: 12,
                seconds: 1,
            });

            const expected = moment.duration({weeks: 2});

            expect(roundDuration(duration).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('1w 6d 11h 59m 59s -> 1w 6d', () => {
            const duration = moment.duration({
                weeks: 1,
                days: 6,
                hours: 11,
                minutes: 59,
                seconds: 59,
            });

            const expected = moment.duration({weeks: 1, days: 6});

            expect(roundDuration(duration).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('1w 0d 12h 0m 1s -> 1w 1d', () => {
            const duration = moment.duration({
                weeks: 1,
                hours: 12,
                seconds: 1,
            });

            const expected = moment.duration({weeks: 1, days: 1});

            expect(roundDuration(duration).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('59m 30s 1ms -> 1h', () => {
            const duration = moment.duration({
                minutes: 59,
                seconds: 30,
                milliseconds: 1,
            });

            const expected = moment.duration({hours: 1});

            expect(roundDuration(duration).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('59m 29s 999ms -> 59m', () => {
            const duration = moment.duration({
                minutes: 59,
                seconds: 29,
                milliseconds: 999,
            });

            const expected = moment.duration({minutes: 59});

            expect(roundDuration(duration).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('При 1d 15h 30m 40s с maxUnits = 2 по умолчанию -> 1d 16h', () => {
            const duration = moment.duration({
                days: 1,
                hours: 15,
                minutes: 30,
                seconds: 40,
            });

            const expected = moment.duration({
                days: 1,
                hours: 16,
            });

            expect(roundDuration(duration).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('При 1d 15h 30m 40s и при переданном opts.maxUnits = 3 -> 1d 15h 31m', () => {
            const opts = {maxUnits: 3};
            const duration = moment.duration({
                days: 1,
                hours: 15,
                minutes: 30,
                seconds: 40,
            });

            const expected = moment.duration({
                days: 1,
                hours: 15,
                minutes: 31,
            });

            expect(roundDuration(duration, opts).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('При 15h 30m 40s и при переданном opts.maxUnits = 3 -> 15h 31m', () => {
            const opts = {maxUnits: 3};
            const duration = moment.duration({
                hours: 15,
                minutes: 30,
                seconds: 40,
            });

            const expected = moment.duration({
                hours: 15,
                minutes: 31,
            });

            expect(roundDuration(duration, opts).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });

        it('При 30m 40s и при переданном opts.maxUnits = 3 -> 31m', () => {
            const opts = {maxUnits: 3};
            const duration = moment.duration({
                minutes: 30,
                seconds: 40,
            });

            const expected = moment.duration({
                minutes: 31,
            });

            expect(roundDuration(duration, opts).asMilliseconds()).toBe(
                expected.asMilliseconds(),
            );
        });
    });
});
