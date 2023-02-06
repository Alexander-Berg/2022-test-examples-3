const path = require('path');

const stylelint = require('stylelint');
const {
    getBlocksMatchers,
} = require('./check-selectors');

const checkCode = async(code, filepath) => {
    const pluginOptions = {
        blocks: ['Button'],
        blocksFile: [path.join(__dirname, 'test.blocks.json')],
    };

    const options = {
        code,
        codeFilename: filepath,
        config: {
            plugins: [path.join(__dirname, 'index.js')],
            rules: {
                '@yandex-lego/stylelint-strict-bem-selector': [true, pluginOptions],
            },
        },
        syntax: 'css',
    };

    const output = await stylelint.lint(options);
    return output.results[0].warnings.length === 0;
};

describe('check-selectors', () => {
    test('block in selector', () => {
        const { hasBlockRulesInSelector } = getBlocksMatchers(['Button', 'Select']);

        expect(hasBlockRulesInSelector('.Button2 ')).toBe(true);
        expect(hasBlockRulesInSelector('.Button2 .Exam')).toBe(true);
        expect(hasBlockRulesInSelector('.Button ')).toBe(true);
        expect(hasBlockRulesInSelector('.Button-Text ')).toBe(true);
        expect(hasBlockRulesInSelector('.ButtonSelect')).toBe(false);
        expect(hasBlockRulesInSelector('.Button2Select')).toBe(false);
    });

    test('check block legal usage in selector', () => {
        const { hasOtherBlockRulesInSelector } = getBlocksMatchers(['Button', 'Select']);

        expect(hasOtherBlockRulesInSelector('.Button2', 'Button')).toBe(false);
        expect(hasOtherBlockRulesInSelector('.Button2 .Button2-Text', 'Button')).toBe(false);
        expect(hasOtherBlockRulesInSelector('.Button2 .Select', 'Button')).toBe(true);
        expect(hasOtherBlockRulesInSelector('.Button2 .Button2-Text .Select-Text', 'Button')).toBe(true);
    });

    test('intergration tests', async() => {
        const valid = [
            ['.Button2 {}', './blocks/Button/_theme/Button_theme_ok.css'],
            ['.Button2 {}', './blocks/Button/_theme/Button_theme_ok@desktop.css'],
            ['.Button2-Text {}', '/Button/_theme/Button_theme_ok'],
            ['.ButtonSelect {}', '/SomeBlock.css'],
            ['.Select {}', '/Select_mod_name.css'],
        ];

        const invalid = [
            ['.Button2 {}', 'ButtonTheme_ok.scss'],
            ['.Select {}', '/Button/asdsad_theme/Button_theme_ok'],
            ['.Button2Select .Select {}', '/SomeBlock.css'],
            ['.Select {}', '/Select.css'],
            ['.SomeBlock.Select {}', '/SomeBlock.css'],
        ];

        for (let [code, filepath] of valid) {
            expect(await checkCode(code, filepath)).toBe(true);
        }

        for (let [code, filepath] of invalid) {
            expect(await checkCode(code, filepath)).toBe(false);
        }
    });
});
