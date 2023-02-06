import pluralize from './pluralize';

const testWordForms = {
    nominative: 'число',
    genitive: 'числа',
    plural: 'чисел',
};

const testCases = [
    {
        inputCount: 1,
        output: 'число',
    },
    {
        inputCount: 2,
        output: 'числа',
    },
    {
        inputCount: 5,
        output: 'чисел',
    },
];

describe('pluralize', () => {
    testCases.forEach((tc) => {
        const { inputCount, output } = tc;
        const pluralizeForm = pluralize(inputCount, testWordForms);

        test(`"${inputCount}" => "${output}"`, () => {
            expect(pluralizeForm).toBe(output);
        });
    });
});
