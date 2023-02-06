var expect = require('chai').expect;
var DetectorPatterns = require('../src/DetectorPatterns').DetectorPatterns;

describe('DetectorPatterns', function () {

    it('Matching ', function () {
        var patterns = DetectorPatterns.fromXML([
            {
                _: 'Ubuntu',
                '$': {type: 'string', value: 'Ubuntu'}
            }, {
                _: '[^_\\.]10[_\\.]10',
                '$': {type: 'regex', value: 'Mac OS X Yosemite'}
            }
        ]);

        expect(patterns.isMatch('Ubuntu')).to.equal(true);
        expect(patterns.isMatch('Mac 10.10')).to.equal(true);
        expect(patterns.isMatch('test')).to.equal(false);
    });

});
