import extractPrice from './extract-price';

const testCases = [
    {
        input: '',
        output: '',
    },
    {
        input: '54 00',
        output: '5400',
    },
    {
        input: '65.000',
        output: '65000',
    },
    {
        input: '55.00',
        output: '55',
    },
    {
        input: '70,00',
        output: '70',
    },
    {
        input: '100.50',
        output: '101',
    },
    {
        input: '100,50',
        output: '100',
    },
];

describe('extractPrice', () => {
    testCases.forEach((tc) => {
        const { input, output } = tc;

        test(`'${input}' => '${output}'`, () => {
            expect(extractPrice(input)).toBe(output);
        });
    });
});
