const isInteger = require('./is-integer');

const testCases = [
    {
        input: 'abc',
        output: false,
    },
    {
        input: 123,
        output: true,
    },
    {
        input: '100',
        output: true,
    },
    {
        input: Infinity,
        output: false,
    },
];

describe('isInteger returns true if a value is number and finite', () => {
    testCases.forEach(({ input, output }) => {
        test(`${input} => ${output}`, () => {
            expect(isInteger(input)).toBe(output);
        });
    });
});
