const { replaceToken } = require('../../utils/logs');

describe('Logs', () => {
    const data = [
        {
            input:
                'http://example.com?access_token=AgAAAAAOFx*****************************&token_type=bearer&expires_in=15552000',
            output: 'http://example.com?access_token=TOKEN_WAS_HERE&token_type=bearer&expires_in=15552000',
        },
        {
            input: 'text',
            output: 'text',
        },
        {
            input: 'http://example.com?access_token=ffefwefjnwjefwenfenfj&auth_token=wjfewknfjwhefuowiejlkf\\n',
            output: 'http://example.com?access_token=TOKEN_WAS_HERE&auth_token=TOKEN_WAS_HERE',
        },
        {
            input: JSON.stringify(['text', 'http://example.com?query1=1&bearer_token=1&query1=1&query1=1&token=123']),
            output: JSON.stringify([
                'text',
                'http://example.com?query1=1&bearer_token=TOKEN_WAS_HERE&query1=1&query1=1&token=TOKEN_WAS_HERE',
            ]),
        },
        {
            input: JSON.stringify(['text', 'http://example.com?query1=1&bearer_token=1&query1=1&query1=1', 'text']),
            output: JSON.stringify([
                'text',
                'http://example.com?query1=1&bearer_token=TOKEN_WAS_HERE&query1=1&query1=1',
                'text',
            ]),
        },
        {
            input: JSON.stringify({
                url: 'http://example.com?access_token=ffefwefjnwjefwenfenfj&example_query=0222',
                url2: 'http://example.com?example_query=0222&access_token=ffefwefjnwjefwenfenfj',
            }),
            output: JSON.stringify({
                url: 'http://example.com?access_token=TOKEN_WAS_HERE&example_query=0222',
                url2: 'http://example.com?example_query=0222&access_token=TOKEN_WAS_HERE',
            }),
        },
        {
            input: '',
            output: '',
        },
        {
            input: undefined,
            output: undefined,
        },
    ];
    describe('should replace tokens from string', () => {
        data.forEach(({ input, output }, index) => {
            it(`case ${index + 1}`, () => {
                expect(replaceToken(input)).toEqual(output);
            });
        });
    });
});
