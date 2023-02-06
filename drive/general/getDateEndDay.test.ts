import { getDateEndDay } from 'shared/helpers/getDateEndDay/getDateEndDay';

describe('getDateEndDay', function () {
    it('should works', function () {
        expect(getDateEndDay(new Date('2022-01-01'))).toMatchInlineSnapshot(`2022-01-01T23:59:59.999Z`);
    });
});
