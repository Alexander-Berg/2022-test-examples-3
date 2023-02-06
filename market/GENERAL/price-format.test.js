import { priceFormat, priceRangeFormat } from './price-format';

describe('priceFormat', () => {
    // \u00A0 - &nbsp; symbol
    const testCases = [
        {
            inputPrice: 75000,
            inputCurrencyCode: 'руб.',
            output: '75\u00A0000 руб.',
        },
        {
            inputPrice: 50.51,
            inputCurrencyCode: 'руб.',
            output: '50,51 руб.',
        },
    ];

    testCases.forEach((tc) => {
        const { inputPrice, inputCurrencyCode, output } = tc;

        test(`'${inputPrice}' => '${output}'`, () => {
            const formattedPrice = priceFormat(inputPrice, inputCurrencyCode);

            expect(formattedPrice).toBe(output);
        });
    });
});

describe('priceRangeFormat', () => {
    const testCases = [
        {
            inputMinPrice: 55000,
            inputMaxPrice: 75000,
            inputCurrencyCode: 'руб.',
            output: '(55\u00A0000 - 75\u00A0000 руб.)',
        },
        {
            inputMinPrice: 2300,
            inputMaxPrice: 5800,
            inputCurrencyCode: 'руб.',
            output: '(2\u00A0300 - 5\u00A0800 руб.)',
        },
    ];

    testCases.forEach((tc) => {
        const { inputMinPrice, inputMaxPrice, inputCurrencyCode, output } = tc;

        test(`'${inputMinPrice} - ${inputMaxPrice}' => '${output}'`, () => {
            const formattedPriceRange = priceRangeFormat(inputMinPrice, inputMaxPrice, inputCurrencyCode);

            expect(formattedPriceRange).toBe(output);
        });
    });
});
