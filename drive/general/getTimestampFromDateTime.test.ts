import { getTimestampFromDateTime } from 'shared/helpers/getTimestampFromDateTime/getTimestampFromDateTime';

describe('getTimestampFromDateTime', function () {
    it('should works', function () {
        expect(getTimestampFromDateTime(new Date('2022-01-01'), '00:00')).toMatchInlineSnapshot(`1640995200000`);

        expect(getTimestampFromDateTime(new Date('2022-05-01'), '23:59')).toMatchInlineSnapshot(`1651449540000`);
    });
});
