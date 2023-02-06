import formatRating from './format-rating';

const testCases = [
    {
        input: 5,
        output: '5.0',
    },
    {
        input: 4.5,
        output: '4.5',
    },
];

describe('extractPrice', () => {
    testCases.forEach((tc) => {
        const { input, output } = tc;

        test(`'${input}' => '${output}'`, () => {
            expect(formatRating(input)).toBe(output);
        });
    });
});

describe('when formatToParts is undefined', () => {
    let formatToParts = null;
    beforeAll(() => {
        formatToParts = Intl.NumberFormat.prototype.formatToParts;
        Intl.NumberFormat.prototype.formatToParts = undefined;
    });
    testCases.forEach((tc) => {
        const { input, output } = tc;

        test(`'${input}' => '${output}'`, () => {
            expect(formatRating(input)).toBe(output);
        });
    });
    afterAll(() => {
        Intl.NumberFormat.prototype.formatToParts = formatToParts;
    });
});
