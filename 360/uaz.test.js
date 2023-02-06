'use strict';

jest.useFakeTimers('modern');
jest.setSystemTime(1580554500751);

jest.mock('../../v2/flags/_helpers/get-location.js', () => () => 'FAKE_LOCATION');

const uaz = require('./uaz.js');
const uazMock = require('../../../test/mock/uaz-fake.json');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const status = require('../_helpers/status');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            client: 'iphone',
            uuid: 'deadbeef42'
        },
        request: mockService,
        config: {
            USER_IP: 'FAKE_IP'
        },
        status: status(core)
    };
});

test('-> PERM_FAIL если нет uuid в параметрах', async () => {
    delete core.params.uuid;

    const res = await uaz(core);

    expect(res.status.status).toBe(3);
});

test('-> OK', async () => {
    mockService.mockResolvedValueOnce(uazMock);

    const res = await uaz(core);

    expect(res).toMatchSnapshot();
});

test('дергает сервис с правильными параметрами', async () => {
    mockService.mockResolvedValueOnce(uazMock);

    await uaz(core);

    expect(mockService.mock.calls).toMatchSnapshot();
});

test('отвечаем пустыми экспериментами, если сервис ответил пустым ответом', async () => {
    mockService.mockResolvedValueOnce({
        ExpBoxes: '',
        Handlers: []
    });

    const res = await uaz(core);

    expect(res.status.status).toBe(1);
    expect(res.uaz).toEqual([]);
});

describe('ошибки', () => {
    it('-> PERM_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        return uaz(core).then((res) => {
            expect(res).toMatchSnapshot();
        });
    });

    it('-> TMP_FAIL', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const res = await uaz(core);

        expect(res).toMatchSnapshot();
    });
});
