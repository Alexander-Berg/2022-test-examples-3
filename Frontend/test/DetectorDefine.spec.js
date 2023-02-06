var expect = require('chai').expect;
var DetectorDefine = require('../src/DetectorDefine').DetectorDefine;

describe('DetectorDefine', function () {

    it('value define', function () {
        var pattern = DetectorDefine.fromXML({$: {name: 'BrowserName', value: 'vkShare'}});

        var traits = {};
        pattern.trigger('test', traits);

        expect(traits).to.eql({
            BrowserName: 'vkShare'
        });
    });

    it('pattern define', function () {
        var pattern = DetectorDefine.fromXML({
            $: {name: 'OSVersion'},
            pattern: [{
                _: 'el([0-9][0-9.]*)',
                $: {type: 'regex', value: '$1'}
            }]
        });

        var traits = {};
        pattern.trigger('el12', traits);

        expect(traits).to.eql({
            OSVersion: '12'
        });
    });

});
