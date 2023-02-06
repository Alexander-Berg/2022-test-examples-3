import { isSameDate } from 'shared/helpers/isSameDate/isSameDate';

describe('isSameDate', function () {
    it('works with same dates', function () {
        expect(isSameDate(new Date('2022-01-01'), new Date('2022-01-01'))).toBeTruthy();
    });

    it('works with different dates', function () {
        expect(isSameDate(new Date('2022-01-01'), new Date('2021-01-01'))).toBeFalsy();
    });
});
