import request from 'supertest';
import nock from 'nock';

import app from '../app';
import { config } from '../lib/config';
import { logger } from '../lib/logger';
import { deepFreeze, mockNetwork, defaultBlackboxUid } from '../utils/tests';

describe('Router. Market-pers-address', () => {
    const networkMock = mockNetwork();

    const addressId = 'chaos-1';
    const marketData = deepFreeze([{ address: 'address' }]);

    beforeEach(() => {
        networkMock.mockTvm();
    });

    afterEach(() => {
        expect(networkMock.getLog()).toMatchSnapshot();
    });

    describe('Успешный запрос', () => {
        test('GET запрос', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .get(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
                .reply(200, marketData);

            await request(app)
                .get('/v1/market-pers-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(200, marketData);
        });

        test('POST запрос', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .post(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`, { country: 'juniper' })
                .reply(200, marketData);

            await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(200, marketData);
        });

        test('PUT запрос', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(200, marketData);

            await request(app)
                .put(`/v1/market-pers-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(200, marketData);
        });

        test('DELETE запрос', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .delete(`/address/uid/${defaultBlackboxUid}/${addressId}?source=${config.persAddress.source}`)
                .reply(200, marketData);

            await request(app)
                .delete(`/v1/market-pers-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(200, marketData);
        });
    });

    describe('Должен вернуть 401 без заголовка авторизации', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('GET запрос', async() => {
            await request(app)
                .get('/v1/market-pers-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });
    });

    describe('Должен вернуть 401 при ответе паспорта о невалидной авторизации', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: false });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('GET запрос', async() => {
            await request(app)
                .get('/v1/market-pers-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });
    });

    describe('Должен падать при 500 ответе из паспорта', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: false, fatal: true });

            spyLogger = jest.spyOn(logger, 'error');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('GET запрос', async() => {
            const response = await request(app)
                .get('/v1/market-pers-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('POST запрос', async() => {
            const response = await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('PUT запрос', async() => {
            const response = await request(app)
                .put('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('DELETE запрос', async() => {
            const response = await request(app)
                .delete('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });
    });

    describe('Должен падать при 500 в ручке маркета', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'error');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(2);
        });

        test('GET запрос', async() => {
            networkMock
                .mockPersAddress()
                .get(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
                .reply(500);

            await request(app)
                .get('/v1/market-pers-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500, {
                    name: 'HTTPError',
                    status: 500,
                    message: 'Response code 500 (Internal Server Error)',
                });
        });

        test('POST запрос', async() => {
            networkMock
                .mockPersAddress()
                .post(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`, { country: 'juniper' })
                .reply(500);

            await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500, {
                    name: 'HTTPError',
                    status: 500,
                    message: 'Response code 500 (Internal Server Error)',
                });
        });

        test('PUT запрос', async() => {
            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(500);

            await request(app)
                .put(`/v1/market-pers-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500, {
                    name: 'HTTPError',
                    status: 500,
                    message: 'Response code 500 (Internal Server Error)',
                });
        });

        test('DELETE запрос', async() => {
            networkMock
                .mockPersAddress()
                .delete(`/address/uid/${defaultBlackboxUid}/${addressId}?source=${config.persAddress.source}`)
                .reply(500);

            await request(app)
                .delete(`/v1/market-pers-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500, {
                    name: 'HTTPError',
                    status: 500,
                    message: 'Response code 500 (Internal Server Error)',
                });
        });
    });

    describe('Должен отдавать 404 при 404 в ручке маркета', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(2);
        });

        test('PUT запрос', async() => {
            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(404);

            await request(app)
                .put(`/v1/market-pers-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(404, {
                    name: 'HTTPError',
                    status: 404,
                    message: 'Response code 404 (Not Found)',
                });
        });

        test('DELETE запрос', async() => {
            networkMock
                .mockPersAddress()
                .delete(`/address/uid/${defaultBlackboxUid}/${addressId}?source=${config.persAddress.source}`)
                .reply(404);

            await request(app)
                .delete(`/v1/market-pers-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(404, {
                    name: 'HTTPError',
                    status: 404,
                    message: 'Response code 404 (Not Found)',
                });
        });
    });

    describe('Должен отдавать 400 при 400 в ручке маркета', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(2);
        });

        test('POST запрос', async() => {
            networkMock
                .mockPersAddress()
                .post(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`, { country: 'juniper' })
                .reply(400);

            await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(400, {
                    name: 'HTTPError',
                    status: 400,
                    message: 'Response code 400 (Bad Request)',
                });
        });

        test('PUT запрос', async() => {
            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(400);

            await request(app)
                .put(`/v1/market-pers-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(400, {
                    name: 'HTTPError',
                    status: 400,
                    message: 'Response code 400 (Bad Request)',
                });
        });
    });

    describe('Должен падать при отсутствии тикета TVM для blackbox', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            nock.cleanAll();

            networkMock.mockTvm({ blackboxErr: true });
            networkMock.mockBlackbox({ isValid: false });

            spyLogger = jest.spyOn(logger, 'error');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('GET запрос', async() => {
            const response = await request(app)
                .get('/v1/market-pers-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('POST запрос', async() => {
            const response = await request(app)
                .post('/v1/market-pers-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('PUT запрос', async() => {
            const response = await request(app)
                .put('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('DELETE запрос', async() => {
            const response = await request(app)
                .delete('/v1/market-pers-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Authorization', 'OAuth test-oauth-token')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });
    });
});
