import getQueryParameter from './get-query-parameter';

const testCases = [
    {
        title: 'Empty strings',
        input: '',
        inputKey: '',
        output: '',
    },
    {
        title: 'URL with ymclid',
        input:
            // eslint-disable-next-line max-len
            'https://shop-example.com/?utm_source=test_source&utm_campaign=test_campaign&utm_medium=test_medium&utm_content=test_content&utm_term=123456&ymclid=12300045600078900012300045',
        inputKey: 'ymclid',
        output: '12300045600078900012300045',
    },
    {
        title: 'URL with yclid',
        input:
            // eslint-disable-next-line max-len
            'https://shop-example.com/?utm_source=test_source&utm_campaign=test_campaign&utm_medium=test_medium&utm_content=test_content&utm_term=123456&yclid=12300045600078900012300045',
        inputKey: 'yclid',
        output: '12300045600078900012300045',
    },
];

describe('getQueryParameter', () => {
    testCases.forEach((tc) => {
        const { input, inputKey, output, title } = tc;

        test(`'${title}' => '${output}'`, () => {
            expect(getQueryParameter(input, inputKey)).toBe(output);
        });
    });
});
