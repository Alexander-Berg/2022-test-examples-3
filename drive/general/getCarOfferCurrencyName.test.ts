import { getCarOfferCurrencyName } from 'entities/Car/helpers/getCarOfferCurrencyName/getCarOfferCurrencyName';

const CURRENCIES = [
    { id: 'id_czk', title: 'CZK' },
    { id: 'id_eur', title: 'EUR' },
];

describe('getCarOfferCurrencyName', function () {
    it('should works', function () {
        expect(getCarOfferCurrencyName('id_eur', CURRENCIES)).toMatchInlineSnapshot(`"EUR"`);
        expect(getCarOfferCurrencyName('id_czk', CURRENCIES)).toMatchInlineSnapshot(`"CZK"`);
        expect(getCarOfferCurrencyName('id_byn', CURRENCIES)).toMatchInlineSnapshot(`"???"`);
    });
});
