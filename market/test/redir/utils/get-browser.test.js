const getBrowser = require('./get-browser');

const testCases = [
    /**
     * TODO: check an empty string
     */
    // {
    //     title: 'An empty string',
    //     input: '',
    //     output: '',
    // },
    {
        title: 'Desktop (Mozilla)',
        input: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:73.0) Gecko/20100101 Firefox/73.0',
        output: { major: '73', name: 'Firefox', version: '73.0' },
    },
    {
        title: 'Desktop (Chrome)',
        input:
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36',
        output: { major: '80', name: 'Chrome', version: '80.0.3987.116' },
    },
];

describe('getBrowser returns browser name and version', () => {
    testCases.forEach(({ title, input, output }) => {
        test(`${title}`, () => {
            expect(getBrowser(input)).toEqual(output);
        });
    });
});
