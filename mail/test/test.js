const assert = require('assert');

const geobase = process._linkedBinding('geobase');

const { GeobaseLookup } = geobase;
const data = process.env.DATA_PATH;

const lookup = new GeobaseLookup(data);

(() => {
    const point = [55.733684, 37.588496];
    const regionId = lookup.getRegionIdByLocation(...point);
    assert.strictEqual(regionId, 120542);
    assert.strictEqual(lookup.getRegionById(regionId).name, 'Хамовники');
}) ();
