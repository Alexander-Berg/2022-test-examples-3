import { formatCarModel } from 'entities/Car/helpers/formatCarModel/formatCarModel';

describe('formatCarModel', function () {
    it('works with empty params', function () {
        expect(formatCarModel('')).toMatchInlineSnapshot(`"—"`);
    });

    it('works with filled params', function () {
        expect(formatCarModel('kio_rio_xline')).toMatchInlineSnapshot(`"Kio Rio Xline"`);
    });
});
