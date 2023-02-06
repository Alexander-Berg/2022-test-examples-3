import { formatMillisecondsToString } from 'shared/helpers/formatMillisecondsToString/formatMillisecondsToString';

/*eslint-disable @typescript-eslint/no-magic-numbers*/
describe('formatMillisecondsToString', function () {
    it('works with empty params', function () {
        expect(formatMillisecondsToString(0)).toMatchInlineSnapshot(`"0 minutes"`);
    });

    it('works with days', function () {
        expect(formatMillisecondsToString(86400000)).toMatchInlineSnapshot(`"1 day"`);
        expect(formatMillisecondsToString(98400000)).toMatchInlineSnapshot(`"1 day 3 hours"`);
    });

    it('works with hours', function () {
        expect(formatMillisecondsToString(3600000)).toMatchInlineSnapshot(`"1 hour"`);
        expect(formatMillisecondsToString(3900000)).toMatchInlineSnapshot(`"1 hour 5 minutes"`);
    });

    it('works with minutes', function () {
        expect(formatMillisecondsToString(300000)).toMatchInlineSnapshot(`"5 minutes"`);
        expect(formatMillisecondsToString(450000)).toMatchInlineSnapshot(`"8 minutes"`);
    });
});
