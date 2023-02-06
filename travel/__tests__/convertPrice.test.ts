import {ICurrenciesInfo} from 'server/services/CurrenciesService/types';

import {convertPriceToPreferredCurrency} from 'utilities/currency/convertPrice';
import {CurrencyType} from 'utilities/currency/CurrencyType';

const price = {
    value: 100,
    currency: CurrencyType.USD,
};

describe('convertPrice', () => {
    describe('Есть данные для конвертации', () => {
        it('Нет курсов для валюты цены', () => {
            const currenciesInfo = {
                nationalCurrency: CurrencyType.RUR,
                preferredCurrency: CurrencyType.UAH,
                currencyRates: {
                    [CurrencyType.UAH]: 3,
                },
            } as ICurrenciesInfo;

            const actualPrice = convertPriceToPreferredCurrency({
                ...price,
                currenciesInfo,
            });

            expect(actualPrice).toEqual(price);
        });

        it('Нет данных для выбранной пользователем валюты', () => {
            const currenciesInfo = {
                nationalCurrency: CurrencyType.RUR,
                preferredCurrency: CurrencyType.UAH,
                currencyRates: {
                    [CurrencyType.USD]: 66,
                },
            } as ICurrenciesInfo;

            const actualPrice = convertPriceToPreferredCurrency({
                ...price,
                currenciesInfo,
            });

            expect(actualPrice).toEqual(price);
        });
    });

    describe('Нет данных для конвертации', () => {
        it('Если валюта цены совпадает с валютой выбранной пользователем - вернём исходную цену', () => {
            const currenciesInfo = {
                nationalCurrency: CurrencyType.RUR,
                preferredCurrency: CurrencyType.USD,
            } as ICurrenciesInfo;
            const result = convertPriceToPreferredCurrency({
                ...price,
                currenciesInfo,
            });

            expect(result).toEqual(price);
        });

        it('Вернёт конвертированную валюту', () => {
            const currenciesInfo = {
                nationalCurrency: CurrencyType.RUR,
                preferredCurrency: CurrencyType.UAH,
                currencyRates: {
                    [CurrencyType.UAH]: 3,
                    [CurrencyType.USD]: 66,
                },
            } as ICurrenciesInfo;

            const actualPrice = convertPriceToPreferredCurrency({
                ...price,
                currenciesInfo,
            });
            const expectedPrice = {
                currency: CurrencyType.UAH,
                value: 2200,
            };

            expect(actualPrice).toEqual(expectedPrice);
        });

        it('Вернёт конвертированную валюту со штрафом', () => {
            const currenciesInfo = {
                nationalCurrency: CurrencyType.RUR,
                preferredCurrency: CurrencyType.UAH,
                currencyRates: {
                    [CurrencyType.UAH]: 3,
                    [CurrencyType.USD]: 66,
                },
            } as ICurrenciesInfo;

            const {value: priceWithFine} = convertPriceToPreferredCurrency({
                ...price,
                currenciesInfo,
                withFine: true,
            });
            const {value: priceWithoutFine} = convertPriceToPreferredCurrency({
                ...price,
                currenciesInfo,
            });

            expect(priceWithFine > priceWithoutFine).toBe(true);
        });
    });
});
