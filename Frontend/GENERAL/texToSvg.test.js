const path = require('path');
const { convertTexToSvg } = require('./texToSvg');
const { matchContent } = require('../utils/test');

const matchSnapshot = matchContent(path.join(process.cwd(), 'tests', 'fixtures', 'svg'));

describe('Convert TeX to SVG', function() {
    it('basic math', async function() {
        const { markup } = await convertTexToSvg('E=mc^{2}');

        await matchSnapshot('basic_math.svg', markup);
    });

    it('prof math', async function() {
        const { markup } = await convertTexToSvg(
            '\\sqrt{\\frac{5}{3x - 58}} = \\frac{1}{10} \\Rightarrow \\frac{5}{3x - 58} = \\left( \\frac{1}{10} \\right)^{2} \\Rightarrow \\frac{5}{3x - 58} = \\frac{1}{100} \\Rightarrow 3x - 58 = 500 \\Rightarrow 3x = 558 \\Rightarrow x = 186'
        );

        await matchSnapshot('prof_math.svg', markup);
    });

    it('basic chem', async function() {
        const { markup } = await convertTexToSvg('\\mathrm{CO_2 \\xrightarrow{X} K_2CO_3\\xrightarrow{Y}KHCO_3}.');

        await matchSnapshot('basic_chem.svg', markup);
    });

    it('inline TeX', async function() {
        const { markup } = await convertTexToSvg(' \\frac{6}{m}', { inline: true });

        await matchSnapshot('inline_tex.svg', markup);
    });

    it('tg operator', async function() {
        const { markup } = await convertTexToSvg('\\sin \\frac{\\pi}{6} + \\tg \\frac{\\pi}{4}', { inline: true });

        await matchSnapshot('tg.svg', markup);
    });
});
