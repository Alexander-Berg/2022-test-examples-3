const { sanitizeQueryString } = require('../../../../src/shared/utils/qs');

const fixtures = [
    {
        description: 'double ampersand',
        input: '&&param1=val&&param2:val',
        output: '&param1=val&param2:val',
    },
    {
        description: 'begins without ampersand',
        input: 'param1=val&param2:val',
        output: '&param1=val&param2:val',
    },
    {
        description: 'new lines',
        input: '&param1=val\n&param2:val',
        output: '&param1=val&param2:val',
    },
    {
        description: 'new lines without ampersands',
        input: 'param1=val\nparam2:val',
        output: '&param1=val&param2:val',
    },
];

describe('/shared/utils/qs', function() {
    describe('sanitizeQueryString should return correct results in case:', function() {
        fixtures.forEach((fixture) => {
            it(fixture.description, () => {
                const actual = sanitizeQueryString(fixture.input);
                const expectations = fixture.output;

                assert.equal(actual, expectations);
            });
        });
    });
});
