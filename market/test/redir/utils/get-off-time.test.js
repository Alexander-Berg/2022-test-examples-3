const getOffTime = require('./get-off-time');

const testCases = [
    {
        input: '',
        output: undefined,
    },
    {
        input: 'half-hour',
        output: 1800000,
    },
    {
        input: 'day',
        output: 86400000,
    },
    {
        input: '3 days',
        output: 259200000,
    },
    {
        input: '7 days',
        output: 604800000,
    },
    {
        input: '30 days',
        output: 2592000000,
    },
];

describe('getOffTime returns the right amount of time in ms', () => {
    testCases.forEach(({ input, output }) => {
        test(`${input} => ${output}`, () => {
            expect(getOffTime(input)).toBe(output);
        });
    });
});
