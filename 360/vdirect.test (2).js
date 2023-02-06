'use strict';

const Vdirect = require('../../build/models/sba/vdirect.js').default;
const vdirect = new Vdirect().action;

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        config: {},
        auth: {
            get: () => ({ login: 'TEST_LOGIN' })
        },
        service: () => mockService
    };
});

test('дергает сервис с правильными параметрами', async () => {
    mockService.mockResolvedValueOnce({ info: [ '42' ] });

    await vdirect({ url: 'https://ya.ru' }, core);

    expect(mockService.mock.calls).toMatchSnapshot();
});

test('выявляет заражённые ссылки', async () => {
    mockService.mockResolvedValueOnce({ info: [ '42' ] });

    const res = await vdirect({ url: 'infected' }, core);

    expect(res).toEqual({ status: 'infected' });
});

test('работает', async () => {
    mockService.mockResolvedValueOnce({ info: [] });

    const res = await vdirect({ url: 'https://ya.ru' }, core);

    expect(res).toEqual({ status: 'clean' });
});

test('обрабатывает ошибки', async () => {
    mockService.mockRejectedValueOnce({});

    const res = await vdirect({ url: 'https://ya.ru' }, core);

    expect(res).toEqual({ status: 'error' });
});
