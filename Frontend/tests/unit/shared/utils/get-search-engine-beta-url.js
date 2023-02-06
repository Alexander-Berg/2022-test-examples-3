const { getSearchEngineBetaUrl } = require('../../../../src/shared/utils/get-search-engine-beta-url');
const fixtures = [
    {
        description: 'with protocol and second-level domain',
        input: 'http://yandex.ru',
        output: 'yandex',
    },
    {
        description: 'with protocol and third-level domain',
        input: 'http://hamster.yandex.ru',
        output: 'hamster.yandex',
    },
    {
        description: 'with protocol and without domain',
        input: 'http://yandex',
        output: 'yandex',
    },
    {
        description: 'without protocol and second-level domain',
        input: 'google.com',
        output: 'google',
    },
    {
        description: 'without protocol and com.tr domain',
        input: 'google.com.tr',
        output: 'google',
    },
    {
        description: 'without protocol and without domain',
        input: 'yandex',
        output: 'yandex',
    },
    {
        description: 'yandex without url',
        engine: 'yandex',
        output: 'yandex',
    },
    {
        description: 'yandex-images without url',
        engine: 'yandex-images',
        output: 'yandex',
    },
    {
        description: 'google without url',
        engine: 'google',
        output: 'google',
    },
    {
        description: 'google-images without url',
        engine: 'google-images',
        output: 'google',
    },
];

describe('shared/utils/getSearchEngineBetaUrl', () => {
    describe('should return correct results in case:', () => {
        fixtures.forEach((fixture) => {
            it(fixture.description, () => {
                const actual = getSearchEngineBetaUrl(fixture.input, fixture.engine);
                const expectations = fixture.output;

                assert.equal(actual, expectations);
            });
        });
    });
});
