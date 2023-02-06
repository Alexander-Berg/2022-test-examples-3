import { getDatesInYear } from 'shared/helpers/getDatesInYear/getDatesInYear';

describe('getDatesInYear', function () {
    it('works with empty params', function () {
        expect(getDatesInYear(new Date('2022-01-01'), new Date('2023-01-01'))).toMatchSnapshot();
    });

    it('works with filled params', function () {
        expect(getDatesInYear(new Date('2000-01-01'), new Date('2000-03-01'))).toMatchSnapshot();
    });
});
