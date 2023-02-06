var expect = require("chai").expect,
    specs = require('./cover.json'),
    detect = require('../index');

describe('uatrits', function () {
    specs.forEach(function (spec) {
        it(spec.ua, function () {
            expect(detect(spec.ua)).to.eql(spec.result);
        });
    });
});
