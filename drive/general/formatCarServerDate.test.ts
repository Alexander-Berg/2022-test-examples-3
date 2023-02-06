import { formatCarServerDate } from 'entities/Car/helpers/formatCarServerDate/formatCarServerDate';

describe('formatCarServerDate', function () {
    it('should word', function () {
        // eslint-disable-next-line @typescript-eslint/no-magic-numbers
        expect(formatCarServerDate(1650693600)).toMatchInlineSnapshot(`2022-04-23T06:00:00.000Z`);
    });
});
