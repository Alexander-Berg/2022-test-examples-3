import { ESLint } from 'eslint';

describe('eslint', () => {
    it('правильно обрабатывает ошибки', async() => {
        const eslint = new ESLint({
            overrideConfig: {
                root: true,
            },
        });

        await eslint.calculateConfigForFile('./eslintrc.js');

        const report = await eslint.lintFiles(['./tests/typescript/fixture.tsx']);

        expect(report[0].messages).toMatchSnapshot();
    });
});
