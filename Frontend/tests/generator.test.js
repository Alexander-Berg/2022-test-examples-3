const helpers = require('yeoman-test');

describe('Nest generator', () => {
    let result = null;

    beforeAll(async () => {
        result = await helpers.create(require.resolve('../generators/nest')).run();
    });

    test('creates common files', () => {
        result.assertFile('package.json');
        result.assertFile('src/client/global/styles/reset.css');
    });
});
