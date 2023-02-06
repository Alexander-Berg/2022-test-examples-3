import { getDateStartDay } from 'shared/helpers/getDateStartDay/getDateStartDay';

describe('getDateStartDay', function () {
    it('should works', function () {
        expect(getDateStartDay(new Date('2022-01-01'))).toMatchInlineSnapshot(`2022-01-01T00:00:00.000Z`);
    });
});
