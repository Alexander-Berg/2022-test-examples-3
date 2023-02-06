const expect = require('chai').expect;
const postcss = require('postcss');
const helpers = require('../helpers/rules');
const style = `.button2.button2_view_default.button2_theme_action.button2_progress_yes:before,
.button2.button2_view_default.button2_action_yes.button2_progress_yes:before {
    background-color: var(--color-bg-progress); /*1*/
    background-image: repeating-linear-gradient(-45deg, var(--color-bg-progress), var(--color-bg-progress) 4px, var(--color-gradient-progress) 4px, var(--color-gradient-progress) 8px);
    background-size: 34px 34px; /*2*/
}`;
const simpleStyle = `.button2.button2_view_default {
    background-color: var(--color-bg-progress); /*1*/
}`;

const varDecl = `.button2_tone_blue {
    --color-bg: #fff;
}`;

const {
    trimRule,
    commentRule,
    commentDecl,
    prepareForTemplate,
} = helpers;

function use(fn, style) {
    return postcss(style => {
        style.walkRules(rule => fn('button2', rule));
    }).process(style).css;
}

describe('rule-helpers', () => {
    it('trims css-rule correctly', () => {
        const result = use(trimRule, style);

        expect(result).to.equal(
            '.button2.button2_view_default.button2_theme_action.button2_progress_yes:before,\n' +
            '.button2.button2_view_default.button2_action_yes.button2_progress_yes:before {\n' +
            '    /* background-color: var(--color-bg-progress); */ /*1*/\n' +
            '    /* background-image: repeating-linear-gradient(-45deg, var(--color-bg-progress), var(--color-bg-progress) 4px, var(--color-gradient-progress) 4px, var(--color-gradient-progress) 8px); */\n' +
            '    background-size: 34px 34px; /*2*/' +
            '\n}');
    });

    it('trims short css-rule correctly', () => {
        const result = use(trimRule, simpleStyle);

        expect(result).to.equal(
            '/*.button2.button2_view_default {\n' +
            '    background-color: var(--color-bg-progress);\n' +
            '}*/');
    });

    it('prepares css-rule for template', () => {
        const result = use(prepareForTemplate, style);

        expect(result.toString()).to.equal(
            '.button2.button2_view_default.button2_tone_${x.tone}.button2_theme_action.button2_progress_yes:before,\n' +
            '.button2.button2_view_default.button2_tone_${x.tone}.button2_action_yes.button2_progress_yes:before {\n' +
            '    background-color: ${check(\'--color-bg-progress\', x)};\n' +
            '    background-image: repeating-linear-gradient(' +
                        '-45deg, ' +
                        '${check(\'--color-bg-progress\', x)}, ' +
                        '${check(\'--color-bg-progress\', x)} 4px, ' +
                        '${check(\'--color-gradient-progress\', x)} 4px, ' +
                        '${check(\'--color-gradient-progress\', x)} 8px);\n}');
    });

    it('comments rule', () => {
        const result = commentRule(varDecl);

        expect(result.toString()).to.equal(
            '/*.button2_tone_blue {\n' +
           '    --color-bg: #fff;\n' +
           '}*/');
    });

    it('comments decl', () => {
        const result = commentDecl({
            prop: 'background',
            value: 'transparent',
        }).toString();

        expect(result).to.equal('/* background: transparent; */');
    });
});
