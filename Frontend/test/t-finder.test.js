const expect = require('chai').expect;

const getTemplates = require('../helpers/t-finder');
const testRoot = 'test/test.levels/';

describe('getTemplates', () => {
    it('only common', () => {
        const result = getTemplates('button2', testRoot);
        expect(result.length).to.equal(1, 'only one template');
        expect(result[0].level).to.equal('common.blocks', 'only common');
    });

    it('common and desktop', () => {
        const result = getTemplates('checkbox', testRoot);

        expect(result[0].level).to.equal('common.blocks', 'common');
        expect(result[1].level).to.equal('desktop.blocks', 'desktop');
    });

    it('ie9 case', () => {
        const result = getTemplates('checkbox', testRoot);
        expect(result[1].path.includes('ie9.csst.js')).to.equal(true);
    });

    it('no templates', () => {
        const result = getTemplates('block', testRoot);

        expect(result.length).to.equal(0);
    });
});
