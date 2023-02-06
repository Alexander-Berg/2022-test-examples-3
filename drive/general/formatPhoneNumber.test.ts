import { formatPhoneNumber } from 'shared/helpers/formatPhoneNumber/formatPhoneNumber';

describe('formatPhoneNumber', function () {
    it('works with empty params', function () {
        expect(formatPhoneNumber('')).toBeUndefined();
        expect(formatPhoneNumber(undefined)).toBeUndefined();
    });

    it('works with BY', function () {
        expect(formatPhoneNumber('+375291000000')).toMatchInlineSnapshot(`"+375 29 100 00 00"`);
        expect(formatPhoneNumber('+375290000000')).toMatchInlineSnapshot(`"+375290000000"`);
    });

    it('works with CZ', function () {
        expect(formatPhoneNumber('+420100000000')).toMatchInlineSnapshot(`"+420 100 000 000"`);
        expect(formatPhoneNumber('+420000000000')).toMatchInlineSnapshot(`"+420000000000"`);
    });

    it('works with LT', function () {
        expect(formatPhoneNumber('+37010000000')).toMatchInlineSnapshot(`"+370 100 00000"`);
        expect(formatPhoneNumber('+37000000000')).toMatchInlineSnapshot(`"+37000000000"`);
    });

    it('works with RU', function () {
        expect(formatPhoneNumber('+71000000000')).toMatchInlineSnapshot(`"+7 100 000 00 00"`);
        expect(formatPhoneNumber('+70000000000')).toMatchInlineSnapshot(`"+70000000000"`);
    });

    it('works with UNKNOWN', function () {
        expect(formatPhoneNumber('+0000000000000')).toMatchInlineSnapshot(`"+0000000000000"`);
    });
});
