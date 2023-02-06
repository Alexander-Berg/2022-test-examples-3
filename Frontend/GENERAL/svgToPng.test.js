const path = require('path');
const { convertSvgToPng } = require('./svgToPng');
const { matchBinary, getFixture } = require('../utils/test');

const matchSnapshot = matchBinary(path.join(process.cwd(), 'tests', 'fixtures', 'png'));
const getSvg = getFixture(path.join(process.cwd(), 'tests', 'fixtures', 'svg'));

describe('Convert SVG to PNG', function() {
    it('basic math', async function() {
        const svgContent = getSvg('basic_math.svg');
        const buffer = await convertSvgToPng(svgContent, { height: 100, width: 100 });

        await matchSnapshot('basic_math.png', buffer);
    });

    it('prof math', async function() {
        const svgContent = getSvg('prof_math.svg');
        const buffer = await convertSvgToPng(svgContent, { height: 100, width: 700 });

        await matchSnapshot('prof_math.png', buffer);
    });

    it('basic chem', async function() {
        const svgContent = getSvg('basic_chem.svg');
        const buffer = await convertSvgToPng(svgContent, { height: 100, width: 100 });

        await matchSnapshot('basic_chem.png', buffer);
    });
});
