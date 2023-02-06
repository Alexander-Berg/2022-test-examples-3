var helper = require('../../helpers/stationRouteSearchHelper');
var expect = require('chai').expect;

describe('stationRouteSearchHelper', function () {
    describe('isKilometerStation', function () {
        it('number without letters', function () {
            expect(helper.isKilometerStation('199')).not.ok;
        });

        it('number with english km', function () {
            expect(helper.isKilometerStation('199km')).ok;
        });

        it('number with english kilometer', function () {
            expect(helper.isKilometerStation('199kilometer')).not.ok;
        });

        it('english km only', function () {
            expect(helper.isKilometerStation('km')).not.ok;
        });

        it('number with russian km', function () {
            expect(helper.isKilometerStation('199км')).ok;
        });

        it('number with russian kilometer', function () {
            expect(helper.isKilometerStation('199километр')).not.ok;
        });

        it('russian km only', function () {
            expect(helper.isKilometerStation('км')).not.ok;
        });
    });
});
