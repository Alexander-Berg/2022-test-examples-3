const {
    concatenateDuration,
    validateDateTime,
    parseDateTime,
} = require('./../../../../../middleware/avia-search/models/date-time');

describe('Date time', () => {
    const DATE_TIMES = require('./date-times.json');
    const NOT_VALID_DATE_TIMES = require('./not-valid-date-times.json');

    describe('validator', () => {
        test('should return undefined if date-time is valid', () => {
            DATE_TIMES &&
                Array.isArray(DATE_TIMES) &&
                DATE_TIMES.forEach((data) => {
                    const dateTime = data.dateTime;
                    const errors = validateDateTime(dateTime);

                    expect(errors).toBeUndefined();
                });
        });

        test('should return errors is date-time is not valid', () => {
            NOT_VALID_DATE_TIMES &&
                Array.isArray(NOT_VALID_DATE_TIMES) &&
                NOT_VALID_DATE_TIMES.forEach((data) => {
                    const dateTime = data.dateTime;
                    const errors = validateDateTime(dateTime);

                    expect(errors).toBeDefined();
                });
        });
    });

    describe('parser', () => {
        test('should parse correctly', () => {
            DATE_TIMES &&
                Array.isArray(DATE_TIMES) &&
                DATE_TIMES.forEach((data) => {
                    const dateTime = data.dateTime;
                    const expected = data.expected;
                    const actual = parseDateTime(dateTime);

                    expect(actual).toEqual(expected);
                });
        });

        test('should return null if date-time is not valid', () => {
            NOT_VALID_DATE_TIMES &&
                Array.isArray(NOT_VALID_DATE_TIMES) &&
                NOT_VALID_DATE_TIMES.forEach((data) => {
                    const dateTime = data.dateTime;
                    const actual = parseDateTime(dateTime);

                    expect(actual).toBeNull();
                });
        });
    });

    describe('Duration', () => {
        describe('concatenator', () => {
            test('should concatenate correctly 1 duration', () => {
                const first = {
                    diff: 9000000,
                    days: 0,
                    hours: 2,
                    minutes: 30,
                };

                const actual = concatenateDuration(first);
                const expected = first;

                expect(actual).toEqual(expected);
            });

            test('should concatenate correctly 2 durations', () => {
                const first = {
                    diff: 44280000,
                    days: 0,
                    hours: 12,
                    minutes: 18,
                };

                const second = {
                    diff: 1080000,
                    days: 0,
                    hours: 0,
                    minutes: 18,
                };

                const expected = {
                    diff: 45360000,
                    days: 0,
                    hours: 12,
                    minutes: 36,
                };

                const actual = concatenateDuration(first, second);

                expect(actual).toEqual(expected);
            });
        });

        test('should concatenate correctly 3 durations', () => {
            const first = {
                diff: 44280000,
                days: 0,
                hours: 12,
                minutes: 18,
            };

            const second = {
                diff: 1080000,
                days: 0,
                hours: 0,
                minutes: 18,
            };

            const third = {
                diff: 84240000,
                days: 0,
                hours: 23,
                minutes: 24,
            };

            const expected = {
                diff: 129600000,
                days: 1,
                hours: 12,
                minutes: 0,
            };

            const actual = concatenateDuration(first, second, third);

            expect(actual).toEqual(expected);
        });
    });
});
