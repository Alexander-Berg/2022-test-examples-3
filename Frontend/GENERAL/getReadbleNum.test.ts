import { getReadbleNum } from './getReadbleNum';

describe('getReadbleNum', () => {
    const cases = [
        { input: 1000, output: '1 000' },
        { input: 10000, output: '10 000' },
        { input: 100000, output: '100 000' },
        { input: 1000000, output: '1 000 000' },
        { input: 10000000, output: '10 000 000' },
        { input: 100000000, output: '100 000 000' },
        { input: 1000000000, output: '1 000 000 000' },

        { input: 1000.1, output: '1 000' },
        { input: 1000.9, output: '1 001' },

        { input: 99, output: '99' },
        { input: 99.7, output: '99.7' },
        { input: 99.7777, output: '99.78' },
        { input: 0.7777, output: '0.78' },
        { input: 0.1, output: '0.1' },
        { input: 0.1001, output: '0.1' },
    ];

    cases.forEach(({ input, output }) => {
        it(`должен преобразовать ${input} в "${output}"`, () => {
            expect(getReadbleNum(input)).toBe(output);
        });
    });
});
