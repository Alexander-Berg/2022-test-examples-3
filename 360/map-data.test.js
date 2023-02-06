'use strict';

const model = require('./map-data.js');

const request = jest.fn();
const service = jest.fn();
const region = {
    id: 1,
    latitude: 20,
    longitude: 30,
    latitude_size: 1,
    longitude_size: 2
};
const core = {
    request,
    service: () => service,
    config: { USER_IP: '0.0.0.0' }
};

beforeEach(() => {
    request
        .mockReturnValueOnce(region)
        .mockResolvedValue('geoInfo');
    service.mockResolvedValue('geocoder');
});

test('use get-address-data/v0 method', async () => {
    const res = await model({ address: 'a' }, core);
    expect(res).toEqual('geoInfo');
    expect(request).lastCalledWith('get-address-data/v0', expect.any(Object));
});

test('use get-address-data/v0 method. Catch error', async () => {
    request.mockRejectedValue({});
    const res = await model({ address: 'a' }, core);
    expect(res).toEqual({});
    expect(request).lastCalledWith('get-address-data/v0', expect.any(Object));
});

test('skips if no region', async () => {
    request.mockReset();
    request.mockReturnValueOnce({});
    const res = await model({ address: 'a' }, core);
    expect(res).toEqual({});
});
