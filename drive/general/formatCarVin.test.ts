import { formatCarVin } from 'entities/Car/helpers/formatCarVin/formatCarVin';

describe('formatCarVin', function () {
    it('works with empty params', function () {
        expect(formatCarVin('')).toMatchInlineSnapshot(`"—"`);
    });

    it('works with full params', function () {
        expect(formatCarVin('5n1an08w75c619694')).toMatchInlineSnapshot(`"5N1AN08W75C619694"`);
        expect(formatCarVin('5n1an08w75c619694', true)).toMatchInlineSnapshot(`"VIN ...619694"`);
    });
});
