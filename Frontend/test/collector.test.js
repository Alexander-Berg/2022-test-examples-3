const fs = require('fs');
const path = require('path');

const expect = require('chai').expect;
const collectTones = require('../helpers/collector');

describe('collector', () => {
    it('collects tones from file', function() {
        const css = fs.readFileSync(path.resolve(__dirname, './test.levels/common.blocks/button2/_tone/button2_tone.css'));
        const result = collectTones(String(css), {});
        expect(result).to.deep.equal({
            pink: {
                '--color-bg-base': '#fff',
                '--color-text': '#000',
                tone: 'pink',
            },
            yellow: {
                '--color-bg-base': '#000',
                '--color-text': '#fff',
                tone: 'yellow',
            },
        });
    });
});
