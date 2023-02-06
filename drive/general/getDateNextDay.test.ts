import { getDateNextDay } from 'shared/helpers/getDateNextDay/getDateNextDay';

describe('getDateNextDay', function () {
    it('should works', function () {
        expect(getDateNextDay(new Date('2022-01-01'))).toMatchInlineSnapshot(`2022-01-02T00:00:00.000Z`);
    });

    it('should round hours', function () {
        expect(getDateNextDay(new Date('2022-01-01T01:22:19'))).toMatchInlineSnapshot(`2022-01-02T00:00:00.000Z`);
    });
});
