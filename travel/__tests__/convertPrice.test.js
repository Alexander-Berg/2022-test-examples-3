import CurrencyCode from '../../../interfaces/CurrencyCode';

import convertPrice from '../convertPrice';

const price = {
    value: 100,
    currency: CurrencyCode.usd,
};

describe('convertPrice', () => {
    it("should return a price if it's currency equals to `preferredCurrency`", () => {
        const currencies = {
            nationalCurrency: CurrencyCode.rub,
            preferredCurrency: CurrencyCode.usd,
            currencyRates: {},
        };
        const result = convertPrice(price, currencies);

        expect(result).toBe(price);
    });

    it('should convert price using target currency rates if present', () => {
        const currencies = {
            nationalCurrency: CurrencyCode.rub,
            preferredCurrency: CurrencyCode.uah,
            currencyRates: {
                UAH: {
                    USD: 10,
                },
            },
        };

        const actualPrice = convertPrice(price, currencies);
        const expectedPrice = {
            currency: CurrencyCode.uah,
            value: 1000,
        };

        expect(actualPrice).toEqual(expectedPrice);
    });

    it('should convert price using preferred currency rates if present', () => {
        const currencies = {
            nationalCurrency: CurrencyCode.rub,
            preferredCurrency: CurrencyCode.uah,
            currencyRates: {
                USD: {
                    UAH: 0.1,
                },
            },
        };

        const actualPrice = convertPrice(price, currencies);
        const expectedPrice = {
            currency: CurrencyCode.uah,
            value: 1000,
        };

        expect(actualPrice).toEqual(expectedPrice);
    });

    it('should convert price using national currency rates if both target and preferred currencies rates are not present', () => {
        const currencies = {
            nationalCurrency: CurrencyCode.rub,
            preferredCurrency: CurrencyCode.uah,
            currencyRates: {
                RUB: {
                    USD: 0.01,
                    UAH: 0.1,
                },
            },
        };

        const actualPrice = convertPrice(price, currencies);
        const expectedPrice = {
            currency: CurrencyCode.uah,
            value: 1000,
        };

        expect(actualPrice).toEqual(expectedPrice);
    });

    it('should return price with no rates', () => {
        const currencies = {
            nationalCurrency: CurrencyCode.rub,
            preferredCurrency: CurrencyCode.uah,
            currencyRates: {
                RUR: {
                    UAH: 0.1,
                },
            },
        };

        const actualPrice = convertPrice(price, currencies);

        expect(actualPrice).toBe(price);
    });
});
