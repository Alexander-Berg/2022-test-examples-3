var expect = require('chai').expect;
var DetectorBranch = require('../src/DetectorBranch').DetectorBranch;

describe('DetectorBranch', function () {

    it('matching', function () {
        var pattern = DetectorBranch.fromXML({
            $: {},
            match: [
                {
                    $: {type: 'any'},
                    pattern: [
                        {_: 'vkShare', $: {type: 'string'}}
                    ]
                }
            ]
        });

        expect(pattern.isMatch('test')).to.eql(false);
        expect(pattern.isMatch('vkShare')).to.eql(true);
    });

    it('defines', function () {
        var pattern = DetectorBranch.fromXML({
            $: {name: 'vkShare'},
            define: [{$: {name: 'BrowserName', value: 'vkShare'}}]
        });

        var traits = {};
        pattern.trigger('test', traits);

        expect(traits).to.eql({
            BrowserName: 'vkShare'
        });
    });

    it('Simple branch with match', function () {
        var pattern = DetectorBranch.fromXML({
            $: {name: 'vkShare'},
            match: [{$: {type: 'any'}, pattern: [{_: 'vkShare', $: {type: 'string'}}]}],
            define: [{$: {name: 'BrowserName', value: 'vkShare'}}]
        });

        expect(pattern.trigger('vkShare', {})).to.eql({
            BrowserName: 'vkShare'
        });

        expect(pattern.isMatch('Windows')).to.equal(false);
    });

    it('sub branches', function () {
        var branch = DetectorBranch.fromXML({
            $: {name: 'isRobot'},
            match: [{
                $: {type: 'any'},
                pattern: [
                    {_: 'baiduspider', $: {type: 'string'}},
                    {
                        _: 'bingbot',
                        $: {type: 'string'}
                    },
                    {_: 'googlebot', $: {type: 'string'}},
                    {
                        _: 'google web preview',
                        $: {type: 'string'}
                    }
                ]
            }],
            define: [{$: {name: 'isRobot', value: 'true'}}, {$: {name: 'isBrowser', value: 'false'}}],
            branch: [
                {
                    $: {name: 'facebookexternalhit'},
                    match: [{
                        $: {type: 'any'},
                        pattern: [{_: 'facebookexternalhit/', $: {type: 'string'}}]
                    }],
                    define: [{$: {name: 'BrowserName', value: 'facebookexternalhit'}}]
                }, {
                    $: {name: 'vkShare'},
                    match: [{$: {type: 'any'}, pattern: [{_: 'vkShare', $: {type: 'string'}}]}],
                    define: [{$: {name: 'BrowserName', value: 'vkShare'}}]
                }
            ]
        });

        expect(branch.trigger('googlebot vkShare', {})).to.eql({
            isRobot: true,
            isBrowser: false,
            BrowserName: 'vkShare'
        });
    });

});
