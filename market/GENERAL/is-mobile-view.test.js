/* eslint-disable max-len */

import isMobileView from './is-mobile-view';

const testCases = [
    {
        title: 'Empty string',
        input: '',
        output: false,
    },
    {
        title: 'Desktop',
        input: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:72.0) Gecko/20100101 Firefox/72.0',
        output: false,
    },
    {
        title: 'Android device',
        input: 'Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Mobile Safari/537.36',
        output: true,
    },
    {
        title: 'iPad',
        input: 'Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1',
        output: false,
    },
    {
        title: 'iPhone',
        input: 'Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1',
        output: true,
    },
];

describe('isMobileView', () => {
    testCases.forEach((tc) => {
        const { input, output, title } = tc;

        test(`${title} => ${output}`, () => {
            expect(isMobileView(input)).toBe(output);
        });
    });
});
