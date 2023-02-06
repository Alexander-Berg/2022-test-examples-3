'use strict';

const ava = require('./ava.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });
const catdogResponse = require('../../../test/mock/catdog.json');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        service: () => mockService
    };
    core.status = status(core);
    jest.spyOn(Date, 'now').mockImplementation(() => 123456789000);
});

describe('-> PERM_FAIL когда нет emails в параметрах', () => {
    it('вообще нет', async () => {
        const res = await ava(core);

        expect(res.status.status).toBe(3);
    });

    it('есть, но в странном формате', async () => {
        core.params.emails = [ 'a@b.c' ];

        const res = await ava(core);

        expect(res.status.status).toBe(3);
    });
});

test('-> OK happy path', async () => {
    mockService.mockResolvedValueOnce(catdogResponse);

    core.params.emails = 'avanez@yandex.ru,avanez@ya.ru,some@example.com';

    const result = await ava(core);

    expect(result.status.status).toBe(1);
    expect(result.profiles).toEqual([
        {
            email: 'avanez@yandex.ru',
            profile: {
                name: 'yandex',
                ava_url: 'https://avatars.mds.yandex.net/get-yapic/45848/enc-f30b1307c3fc9f03ab51e1331520a380f5e813d9f1f6963b338808ea54a32738/islands-200',
                update_time: '123456789'
            }
        },
        {
            email: 'avanez@ya.ru',
            profile: {
                name: 'yandex',
                ava_url: 'https://avatars.mds.yandex.net/get-yapic/45848/enc-f30b1307c3fc9f03ab51e1331520a380f5e813d9f1f6963b338808ea54a32738/islands-200',
                update_time: '123456789'
            }
        }
    ]);
});

describe('ошибки', () => {
    beforeEach(() => {
        core.params.emails = 'avanez@ya.ru,nik.ivanov@gmail.com,djdonkey@ya.ru';
    });

    it('-> PERM_FAIL когда сервис отвечает 4xx', async () => {
        mockService.mockRejectedValueOnce(httpError(400));

        const res = await ava(core);

        expect(res.status.status).toBe(3);
    });

    it('-> TMP_FAIL когда сервис отвечает 5xx', async () => {
        mockService.mockRejectedValueOnce(httpError(500));

        const res = await ava(core);

        expect(res.status.status).toBe(2);
    });
});
