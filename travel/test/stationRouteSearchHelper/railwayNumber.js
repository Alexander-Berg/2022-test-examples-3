var helper = require('../../helpers/stationRouteSearchHelper');
var expect = require('chai').expect;

describe('stationRouteSearchHelper', function () {
    describe('isRailwayRouteNumber', function () {
        it('number without letters', function () {
            expect(helper.isRailwayRouteNumber('199')).not.ok;
        });

        it('number with one english letter', function () {
            expect(helper.isRailwayRouteNumber('199w')).ok;
        });

        it('number with two english letters', function () {
            expect(helper.isRailwayRouteNumber('199wr')).ok;
        });

        it('number with three english letters', function () {
            expect(helper.isRailwayRouteNumber('199wur')).not.ok;
        });

        it('number with one russian letter', function () {
            expect(helper.isRailwayRouteNumber('199й')).ok;
        });

        it('number with two russian letters', function () {
            expect(helper.isRailwayRouteNumber('199йц')).ok;
        });

        it('number with three russian letters', function () {
            expect(helper.isRailwayRouteNumber('199йцы')).not.ok;
        });

        it('english letters only', function () {
            expect(helper.isRailwayRouteNumber('qw')).not.ok;
        });

        it('russian letters only', function () {
            expect(helper.isRailwayRouteNumber('йц')).not.ok;
        });
    });
});
