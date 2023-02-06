const postcss = require('postcss');
const expect = require('chai').expect;

const plugin = require('../../../tools/postcss-lego-hovered');

const test = function(input, output) {
    expect(postcss([plugin()]).process(input).css).to.eql(output);
};

describe('postcss-focus', () => {
    it('removes *_hovered_yes selector', () => {
        test('a_hovered_yes {} b {}', 'b {}');
    });

    it('removes *_hovered_yes selector with child stylings', () => {
        test('a_hovered_yes span {} b {}', 'b {}');
    });

    it('removes *_hovered_yes selector with multiple selectors', () => {
        test('a_hovered_yes, b.visible {}', 'b.visible {}');
    });

    it('removes *_hovered_yes selector with compressed selectors', () => {
        test('a_hovered_yes,b.visible {}', 'b.visible {}');
    });

    it('lego example - removes *_hovered_yes selector', () => {
        test('.button2_view_classic.button2_theme_action.button2_hovered_yes:before { background-color: #ffd633 }', '');
    });

    it('lego example - removes *_hovered_yes selector with multiple selectors', () => {
        test(
            '.button2.button2_view_default.button2_checked_yes.button2_hovered_yes:before,.button2.button2_view_default.button2_disabled_yes { opacity: 1 }',
            '.button2.button2_view_default.button2_disabled_yes { opacity: 1 }'
        );
    });
});
