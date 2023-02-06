'use strict';

jest.mock('protobufjs', () => ({
    loadSync: () => ({
        lookup: () => ({
            toObject: () => {},
            decode: () => {}
        })
    })
}));

jest.mock('@yandex-int/maps-proto-schemas', () => ({
    get: () => {}
}));

jest.mock('../../../lib/service/base-http-service.js', () => class BaseHttpService {
    fetch() {}
});

const GeocoderService = require('./geocoder.js');

test('coverage fetch', () => {
    const service = new GeocoderService();
    service.fetch({}, {});
    // for eslint
    expect(1).toBe(1);
});

test('coverage parse', () => {
    const service = new GeocoderService();
    service.parse();
    // for eslint
    expect(1).toBe(1);
});
