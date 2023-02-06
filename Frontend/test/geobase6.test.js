const assert = require('assert');
const GeobaseLookup = require('@yandex-int/geo').GeobaseLookup;
const geobase = require('../dist').default.v6();

describe('Geobase v6 instance', function() {
    it('should be GeobaseLookup instance', function() {
        assert(geobase instanceof GeobaseLookup);
    });

    it('should lookup regionId', function() {
        const region = geobase.getRegionById(213);
        assert.strictEqual(region.name, 'Москва');
    });
});
