import extractCurrencyCode from './extract-currency-code';

const testCases = [
    {
        input: '',
        output: '',
    },
    {
        input: '$335',
        output: 'USD',
    },
    {
        input: '324 USD',
        output: 'USD',
    },
    {
        input: '55.00 ₽',
        output: 'RUB',
    },
    {
        input: '75.00 £',
        output: 'GBP',
    },
    {
        input: '750 GBP',
        output: 'GBP',
    },
];

describe('extractCurrencyCode', () => {
    testCases.forEach((tc) => {
        const { input, output } = tc;

        test(`'${input}' => '${output}'`, () => {
            expect(extractCurrencyCode(input)).toBe(output);
        });
    });
});
