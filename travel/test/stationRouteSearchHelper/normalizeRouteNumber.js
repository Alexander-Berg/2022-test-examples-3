var helper = require('../../helpers/stationRouteSearchHelper');
var expect = require('chai').expect;

describe('stationRouteSearchHelper', function () {
    describe('normalizeRouteNumber', function () {
        it('space', function () {
            expect(helper.normalizeRouteNumber(' \t 199 \n 00  \t')).equal(
                '19900',
            );
        });

        it('lower case', function () {
            expect(helper.normalizeRouteNumber('ЦЫ')).equal('цы');
        });

        it('вагон', function () {
            expect(helper.normalizeRouteNumber('вагон199')).equal('199');
        });

        it('№', function () {
            expect(helper.normalizeRouteNumber('№234')).equal('234');
        });

        it('поезд', function () {
            expect(helper.normalizeRouteNumber('поезд56')).equal('56');
        });

        it('автобус', function () {
            expect(helper.normalizeRouteNumber('автобус33')).equal('33');
        });

        it('minus', function () {
            expect(helper.normalizeRouteNumber('У6-997')).equal('у6997');
        });

        it('complex train', function () {
            expect(helper.normalizeRouteNumber(' \t поезд № 45-ЦЫ \n')).equal(
                '45цы',
            );
        });

        it('complex vagon', function () {
            expect(helper.normalizeRouteNumber(' \t вагон № 45-ЦЫ \n')).equal(
                '45цы',
            );
        });

        it('complex bus', function () {
            expect(helper.normalizeRouteNumber(' \t автобус № 45-ЦЫ \n')).equal(
                '45цы',
            );
        });
    });
});
