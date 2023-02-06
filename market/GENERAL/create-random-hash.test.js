import createRandomHash from './create-random-hash';

const testCases = [
    {
        input: 0,
        outputLength: 0,
    },
    {
        input: 5,
        outputLength: 5,
    },
    {
        input: 7,
        outputLength: 7,
    },
    {
        input: 10,
        outputLength: 10,
    },
    {
        input: 15,
        outputLength: 15,
    },
];

describe('createRandomHash', () => {
    testCases.forEach((tc) => {
        const { input, outputLength } = tc;
        const got = createRandomHash(input);

        test(`Should be a string consisting of ${input} characters => '${got}'`, () => {
            expect(got.length).toBe(outputLength);
            expect(typeof got).toBe('string');
        });
    });
});
