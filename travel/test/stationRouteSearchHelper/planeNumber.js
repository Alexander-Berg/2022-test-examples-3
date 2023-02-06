var helper = require('../../helpers/stationRouteSearchHelper');
var expect = require('chai').expect;

describe('stationRouteSearchHelper', function () {
    describe('isPlaneRouteNumber', function () {
        describe('english letters', function () {
            it('letter + digit', function () {
                expect(helper.isPlaneRouteNumber('w9')).not.ok;
            });

            it('letter + 2 digits', function () {
                expect(helper.isPlaneRouteNumber('w98')).ok;
            });

            it('2 letters + digits', function () {
                expect(helper.isPlaneRouteNumber('wr199')).ok;
            });

            it('3 letters + number', function () {
                expect(helper.isPlaneRouteNumber('wur199')).not.ok;
            });

            it('digit + letter + digits', function () {
                expect(helper.isPlaneRouteNumber('2w199')).ok;
            });

            it('2 digits + letter + digits', function () {
                expect(helper.isPlaneRouteNumber('23w199')).not.ok;
            });

            it('2 digits + 2 letters', function () {
                expect(helper.isPlaneRouteNumber('23wq')).not.ok;
            });

            it('digit + 2 letters + digits', function () {
                expect(helper.isPlaneRouteNumber('2wq199')).not.ok;
            });

            it('english letters only', function () {
                expect(helper.isPlaneRouteNumber('qw')).not.ok;
            });
        });

        describe('russian letters', function () {
            it('letter + digit', function () {
                expect(helper.isPlaneRouteNumber('й9')).not.ok;
            });

            it('letter + 2 digits', function () {
                expect(helper.isPlaneRouteNumber('й19')).ok;
            });

            it('2 letters + digits', function () {
                expect(helper.isPlaneRouteNumber('йц199')).ok;
            });

            it('3 letters + digits', function () {
                expect(helper.isPlaneRouteNumber('йцы199')).not.ok;
            });

            it('digit + letter + digits', function () {
                expect(helper.isPlaneRouteNumber('2й199')).ok;
            });

            it('2 digits + letter + digits', function () {
                expect(helper.isPlaneRouteNumber('23й199')).not.ok;
            });

            it('2 digits + 2 letters', function () {
                expect(helper.isPlaneRouteNumber('23йц')).not.ok;
            });

            it('letters only', function () {
                expect(helper.isPlaneRouteNumber('йц')).not.ok;
            });
        });

        it('number without letters', function () {
            expect(helper.isPlaneRouteNumber('199')).not.ok;
        });
    });
});
