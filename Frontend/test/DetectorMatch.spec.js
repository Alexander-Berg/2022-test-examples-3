var expect = require('chai').expect;
var DetectorMatch = require('../src/DetectorMatch').DetectorMatch;

describe('DetectorMatch', function () {

    it('any matcher', function () {
        var pattern = DetectorMatch.fromXML({
            '$': {'type': 'any'},
            'pattern': [
                {
                    '_': 'IEMobile',
                    '$': {'type': 'string'}
                },
                {
                    '_': 'IE Mobile',
                    '$': {'type': 'string'}
                },
                {
                    '_': 'MSIE',
                    '$': {'type': 'string'}
                }
            ]
        });

        expect(pattern.isMatch('IEMobile')).to.eql(true);
        expect(pattern.isMatch('IE Mobile')).to.eql(true);
        expect(pattern.isMatch('MSIE')).to.eql(true);
        expect(pattern.isMatch('FF')).to.eql(false);
    });

});
