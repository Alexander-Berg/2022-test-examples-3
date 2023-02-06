import { isDateWeekend } from 'shared/helpers/isDateWeekend/isDateWeekend';

describe('isDateWeekend', function () {
    it("should be falsy; it's Friday", function () {
        expect(isDateWeekend(new Date('2021-10-22'))).toBeFalsy();
    });

    it("should be truthy; it's Saturday", function () {
        expect(isDateWeekend(new Date('2021-10-23'))).toBeTruthy();
    });
});
