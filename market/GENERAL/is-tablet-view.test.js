/* eslint-disable max-len */

import { isTablet } from './is-tablet-view';

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
        title: 'iPhone',
        input: 'Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1',
        output: false,
    },
    {
        title: 'iPad',
        input: 'Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1',
        output: true,
    },
    {
        title: 'Android Tablet',
        input: 'Mozilla/5.0 (Linux; Android 4.3; Nexus 7 Build/JSS15Q) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36',
        output: true,
    },
    {
        title: 'Android Phone',
        input: 'Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Mobile Safari/537.36',
        output: false,
    },
    {
        title: 'TabletYaBro',
        input: 'Mozilla/5.0 (iPad; CPU OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 YaBrowser/21.6.0.1359.01 Mobile/15E148 Safari/604.1',
        output: true,
    },
];

describe('isTablet', () => {
    testCases.forEach((tc) => {
        const { input, output, title } = tc;

        test(`${title} => ${output}`, () => {
            expect(isTablet(input)).toBe(output);
        });
    });
});
