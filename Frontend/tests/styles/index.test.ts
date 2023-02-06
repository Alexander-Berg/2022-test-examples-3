import { lint } from 'stylelint';

describe('stylelint', () => {
    it('правильно обрабатывает ошибки', async() => {
        const results = await lint({
            configFile: './stylelintrc.js',
            files: './tests/styles/fixture.pcss',
            ignoreDisables: true,
        });

        expect(results.results[0].warnings).toMatchSnapshot();
    });
});
