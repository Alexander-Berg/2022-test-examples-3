const moment = require('../../../../reexports').momentTimezone;
const {time, dateTime} = require.requireActual('../time');

const timezone = 'Asia/Yekaterinburg';

describe('sorting by time and dateTime functions', () => {
    describe('sorting by time', () => {
        it('should return 0 on object with same time', () => {
            const result = time.comparator(
                {moment: moment.tz('2016-02-14T00:00:00+05:00', timezone)},
                {moment: moment.tz('2016-02-12T00:00:00+05:00', timezone)},
            );

            expect(result).toBe(0);
        });

        it('should return 1', () => {
            const result = time.comparator(
                {moment: moment.tz('2016-02-14T00:30:00+05:00', timezone)},
                {moment: moment.tz('2016-02-15T00:20:00+05:00', timezone)},
            );

            expect(result).toBeGreaterThan(0);
        });

        it('should return -1', () => {
            const result = time.comparator(
                {moment: moment.tz('2016-02-16T00:20:00+05:00', timezone)},
                {moment: moment.tz('2016-02-14T00:30:00+05:00', timezone)},
            );

            expect(result).toBeLessThan(0);
        });
    });

    describe('sorting by date time', () => {
        it('should return 0 on object with same date', () => {
            const result = dateTime.comparator(
                {datetime: '2016-02-14T00:00:00+05:00'},
                {datetime: '2016-02-14T00:00:00+05:00'},
            );

            expect(result).toBe(0);
        });

        it('should return 1', () => {
            const result = dateTime.comparator(
                {datetime: '2016-02-14T00:00:00+05:00'},
                {datetime: '2016-02-13T23:00:00+05:00'},
            );

            expect(result).toBeGreaterThan(0);
        });

        it('should return -1', () => {
            const result = dateTime.comparator(
                {datetime: '2016-02-14T00:00:00+05:00'},
                {datetime: '2016-02-15T00:00:00+05:00'},
            );

            expect(result).toBeLessThan(0);
        });
    });
});
