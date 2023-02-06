import getHostname from './get-hostname';

const testCases = [
    {
        input: document,
        website: 'www.example.com',
        output: 'example.com',
    },
    {
        input: document,
        website: 'example.com',
        output: 'example.com',
    },
];

describe('getHostname', () => {
    testCases.forEach((tc) => {
        const { input, output, website } = tc;

        input.domain = website;

        test(`'${input.domain}' => '${output}'`, () => {
            expect(getHostname(input)).toBe(output);
        });
    });
});
