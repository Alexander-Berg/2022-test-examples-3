'use strict';

let core;
const service = require('./ocr.js');

beforeEach(() => {
    core = {
        config: {
            services: {
                ocr: 'http://ocr'
            }
        },
        got: jest.fn().mockResolvedValue({})
    };
});

test('идет в сервис ocr', async () => {
    await service(core, '/method');

    expect(core.got.mock.calls[0][0]).toEqual('http://ocr');
});

test('идет в сервис ocr с правильными опциями', async () => {
    const params = {
        foo: 'bar'
    };

    await service(core, '/method', params);

    expect(core.got.mock.calls[0]).toMatchSnapshot();
});
