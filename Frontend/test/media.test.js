const expect = require('chai').expect;
const postcss = require('postcss');

const MediaCollector = require('../helpers/media');
const style = `@media all and (min-width:0) {
    .button2_view_default.button2_theme_normal .button2__text { /*1*/
        color: var(--color-text);
    }
}

@media all and (min-width:100) {
    .button2_view_default.button2_theme_normal .button2:before { /*3*/
        background-color: var(--bg-color);
    }
}`;

describe('media', () => {
    it('media', () => {
        const block = 'button2';
        const media = new MediaCollector();
        const template = [];

        postcss(function(style) {
            style.walkRules(rule => {
                const parent = rule.parent;
                media.rules = { block, parent };
            });

            Object.keys(media.rules).forEach(ruleName => {
                template.push(media.rules[ruleName]);
            });
        }).process(style).css;

        const result = template.join('');

        expect(result).to.equal(
            '@media all and (min-width:0) {\n' +
            '    .button2_view_default.button2_tone_${x.tone}.button2_theme_normal .button2__text {\n' +
            '        color: ${check(\'--color-text\', x)};\n' +
            '    }' +
            '\n}' +
            '@media all and (min-width:100) {\n' +
            '    .button2_view_default.button2_tone_${x.tone}.button2_theme_normal .button2:before {\n' +
            '        background-color: ${check(\'--bg-color\', x)};\n' +
            '    }\n}');
    });
});
