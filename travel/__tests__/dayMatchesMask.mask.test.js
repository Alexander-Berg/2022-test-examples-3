const moment = require.requireActual('../../../../reexports').momentTimezone;
const {dayMatchesMask, EMPTY_DAY, NOT_EMPTY_DAY} =
    require.requireActual('../mask');

const mask = {
    2016: {
        0: new Array(31).map(() => NOT_EMPTY_DAY),
    },
};

mask[2016][0][1] = EMPTY_DAY;

describe('dayMatchesMask', () => {
    it('should return `false` if the year of the day is not present in the mask', () => {
        const day = moment([2017, 1, 1]);

        expect(dayMatchesMask(day, mask)).toBe(false);
    });

    it('should return `false` if the month of the day is not preset in the mask', () => {
        const day = moment([2016, 1, 1]);

        expect(dayMatchesMask(day, mask)).toBe(false);
    });

    it('should return `false` if the day is represented by `EMPTY_DAY` value in the mask', () => {
        const day = moment([2016, 0, 2]);

        expect(dayMatchesMask(day, mask)).toBe(false);
    });

    it('should return `true` if the day is represented by `NOT_EMPTY_DAY` value in the mask', () => {
        const day = moment([2016, 0, 1]);

        expect(dayMatchesMask(day, mask)).toBe(false);
    });
});
