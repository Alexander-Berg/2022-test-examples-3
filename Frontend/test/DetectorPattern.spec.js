var expect = require('chai').expect;
var DetectorPattern = require('../src/DetectorPattern').DetectorPattern;

describe('DetectorPattern', function () {

    it('Should work with strings', function () {
        var pattern = DetectorPattern.fromXML({
            _: 'Ubuntu',
            '$': {type: 'string', value: 'Ubuntu'}
        });

        expect(pattern.match('Ubuntu')).to.equal('Ubuntu');
        expect(pattern.match('Windows')).to.equal(undefined);
    });

    it('Should work with regexp', function () {
        var pattern = DetectorPattern.fromXML({
            _: '[^_\\.]10[_\\.]10',
            '$': {type: 'regex', value: 'Mac OS X Yosemite'}
        });

        expect(pattern.match('Mac 10.10')).to.equal('Mac OS X Yosemite');
        expect(pattern.match('Mac')).to.equal(undefined);
    });

});
