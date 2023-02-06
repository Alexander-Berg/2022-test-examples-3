let assert = require('assert');

let geobases = require('../dist').default;

describe('Geobase instance', function() {
    it('should throw with invalid version', function() {
        assert.throws(function() {
            geobases.v4();
        }, '');
        assert.throws(function() {
            geobases.v5();
        }, '');
    });

    it('should be singleton for v6', function() {
        let geobase = geobases.v6();
        assert(geobase);
        let geobase_ = geobases.v6();
        assert(geobase_);

        assert.strictEqual(geobase, geobase_);
    });

    it('should throw with invalid database for v6', function() {
        assert.throws(function() {
            let geobase = geobases.v6()({
                deobaseData: '/var/cache/geobase/NEGEOBASA.bin',
            });
            let _yarrr = geobase.regionId('41.223.108.223');
        }, '');
    });
});
