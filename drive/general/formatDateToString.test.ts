import { formatDateToString } from 'shared/helpers/formatDateToString/formatDateToString';

describe('formatDateToString', function () {
    it('works with empty params', function () {
        expect(formatDateToString(null)).toBeUndefined();
    });

    it('works with filled params', function () {
        expect(formatDateToString(new Date('2022-01-01'))).toStrictEqual('2022-01-01');
    });

    it('works with number', function () {
        // eslint-disable-next-line @typescript-eslint/no-magic-numbers
        expect(formatDateToString(1652648000000)).toStrictEqual('2022-05-15');
    });
});
