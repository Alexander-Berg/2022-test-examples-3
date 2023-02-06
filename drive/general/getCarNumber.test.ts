import { getCarNumber } from 'entities/Car/helpers/getCarNumber/getCarNumber';

describe('getCarNumber', function () {
    it('works with empty params', function () {
        expect(getCarNumber()).toMatchInlineSnapshot(`"—"`);
    });

    it('works with number params', function () {
        expect(getCarNumber('В236ЕХ797')).toMatchInlineSnapshot(`"В 236 ЕХ 797"`);
        expect(getCarNumber('В236ЕХ797', '5N1AN08W75C619694')).toMatchInlineSnapshot(`"В 236 ЕХ 797"`);
    });

    it('works with vin params', function () {
        expect(getCarNumber(undefined, '5N1AN08W75C619694')).toMatchInlineSnapshot(`"5N1AN08W75C619694"`);
        expect(getCarNumber(undefined, '5N1AN08W75C619694', true)).toMatchInlineSnapshot(`"VIN ...619694"`);
    });
});
