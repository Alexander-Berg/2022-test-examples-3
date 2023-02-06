import request from 'supertest';
import nock from 'nock';

import app from '../app';
import { config } from '../lib/config';
import { csrf } from '../lib/csrf';
import { logger } from '../lib/logger';
import { deepFreeze, mockNetwork, defaultBlackboxUid } from '../utils/tests';

describe('Router. Profile-addresses', () => {
    const networkMock = mockNetwork();

    const addressId = 'chaos-1';
    const uaMock = 'Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0';
    const marketData = deepFreeze([{ address: 'address', regionId: 2 }]);
    const regionData = deepFreeze({ type: 5, name: 'amber district' });

    beforeEach(() => {
        networkMock.mockTvm();
    });

    afterEach(() => {
        expect(networkMock.getLog()).toMatchSnapshot();
    });

    describe('Успешный запрос', () => {
        let csrfToken: string;

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('GET', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .get(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
                .reply(200, marketData);

            networkMock
                .mockGeobase()
                .get('/v1/region_by_id?id=2')
                .reply(200, regionData)
                .get('/v1/parents?id=2')
                .reply(200, []);

            await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('User-Agent', uaMock)
                .set('Cookie', 'Session_id=12345;')
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(200, [{ ...marketData[0], region: regionData.name }]);
        });

        test('GET с вычислением parent региона', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .get(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
                .reply(200, [{ ...marketData[0], regionId: 5 }]);

            networkMock
                .mockGeobase()
                .get('/v1/region_by_id?id=5')
                .reply(200, { type: 25, name: 'rebma city' })
                .get('/v1/parents?id=5')
                .reply(200, [6, 7])
                .get('/v1/region_by_id?id=6')
                .reply(200, { type: 5, name: 'federal subject' })
                .get('/v1/region_by_id?id=7')
                .reply(200, { type: 4, name: 'federal district' });

            await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('User-Agent', uaMock)
                .set('Cookie', 'Session_id=12345;')
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(200, [{ ...marketData[0], regionId: 5, region: 'federal subject, federal district' }]);
        });

        test('POST', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .post(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`, { country: 'juniper' })
                .reply(200, marketData);

            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(200, marketData);
        });

        test('PUT', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(200, marketData);

            await request(app)
                .put(`/v1/profile-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(200, marketData);
        });

        test('DELETE', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .delete(`/address/uid/${defaultBlackboxUid}/${addressId}?source=${config.persAddress.source}`)
                .reply(200, marketData);

            await request(app)
                .delete(`/v1/profile-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(200, marketData);
        });
    });

    describe('Должен вернуть 401 при отсутствии Session_id куки', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('GET запрос', async() => {
            await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'yandexuid=123')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'yandexuid=123')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'yandexuid=123')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });
    });

    describe('Должен вернуть 401 при ответе паспорта о невалидной авторизации', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: false });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('GET запрос', async() => {
            await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(401, {
                    name: 'UnauthorizedError',
                    status: 401,
                    message: 'Unauthorized',
                });
        });
    });

    describe('Должен падать при 500 ответе из паспорта', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: false, fatal: true });

            spyLogger = jest.spyOn(logger, 'error');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('GET запрос', async() => {
            const response = await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('POST запрос', async() => {
            const response = await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('PUT запрос', async() => {
            const response = await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('DELETE запрос', async() => {
            const response = await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });
    });

    describe('Должен вернуть 400 при отсутствии куки yandexuid', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;')
                .set('X-Csrf-Token', 'csrfToken')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });
    });

    describe('Должен вернуть 400 при отсутствующем csrf токене', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });
    });

    describe('Должен вернуть 400 при неверном csrf токене', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('POST запрос', async() => {
            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', '123')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });

        test('PUT запрос', async() => {
            await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', '123')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });

        test('DELETE запрос', async() => {
            await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', '123')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'BadRequestError',
                    status: 400,
                    message: 'Invalid CSRF token',
                    code: 'INVALID_CSRF_TOKEN',
                });
        });
    });

    describe('Должен падать при 500 в ручке геобазы', () => {
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            spyLogger = jest.spyOn(logger, 'error');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(1);
        });

        test('GET запрос', async() => {
            networkMock.mockBlackbox({ isValid: true });

            networkMock
                .mockPersAddress()
                .get(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
                .reply(200, [{ address: 'address', regionId: 51 }]);

            networkMock
                .mockGeobase()
                .get('/v1/region_by_id?id=51')
                .reply(500);

            await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500, {
                    name: 'HttpGeobase',
                    status: 500,
                    message: 'Response code 500 (Internal Server Error)',
                });
        });
    });

    describe('Должен вернуть 404 при 404 в ручке маркета', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(2);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('PUT запрос', async() => {
            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(404);

            await request(app)
                .put(`/v1/profile-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
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
                .delete(`/v1/profile-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(404, {
                    name: 'HTTPError',
                    status: 404,
                    message: 'Response code 404 (Not Found)',
                });
        });
    });

    describe('Должен вернуть 400 при 400 в ручке маркета', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'warn');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(2);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('PUT запрос', async() => {
            networkMock
                .mockPersAddress()
                .put(`/address/uid/${defaultBlackboxUid}/${addressId}/blue?source=${config.persAddress.source}`, {
                    country: 'juniper',
                })
                .reply(400);

            await request(app)
                .put(`/v1/profile-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'HTTPError',
                    status: 400,
                    message: 'Response code 400 (Bad Request)',
                });
        });

        test('DELETE запрос', async() => {
            networkMock
                .mockPersAddress()
                .delete(`/address/uid/${defaultBlackboxUid}/${addressId}?source=${config.persAddress.source}`)
                .reply(400);

            await request(app)
                .delete(`/v1/profile-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(400, {
                    name: 'HTTPError',
                    status: 400,
                    message: 'Response code 400 (Bad Request)',
                });
        });
    });

    describe('Должен падать при 500 в ручке маркета', () => {
        let csrfToken: string;
        let spyLogger: jest.SpyInstance;

        beforeEach(() => {
            networkMock.mockBlackbox({ isValid: true });

            spyLogger = jest.spyOn(logger, 'error');
        });

        afterEach(() => {
            expect(spyLogger).toHaveBeenCalledTimes(2);
        });

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('GET запрос', async() => {
            networkMock
                .mockPersAddress()
                .get(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
                .reply(500, marketData);

            await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
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
                .reply(500, marketData);

            await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
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
                .reply(500, marketData);

            await request(app)
                .put(`/v1/profile-address/${addressId}`)
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
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
                .reply(500, marketData);

            await request(app)
                .delete(`/v1/profile-address/${addressId}`)
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                .set('X-Csrf-Token', csrfToken)
                .set('User-Agent', uaMock)
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500, {
                    name: 'HTTPError',
                    status: 500,
                    message: 'Response code 500 (Internal Server Error)',
                });
        });
    });

    describe('Должен падать при отсутствии тикета TVM для blackbox', () => {
        let csrfToken: string;
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

        beforeAll(() => {
            csrfToken = csrf.generateToken({ yandexuid: '431818718111111028' });
        });

        test('GET запрос', async() => {
            const response = await request(app)
                .get('/v1/profile-address')
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('X-Csrf-Token', csrfToken)
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('POST запрос', async() => {
            const response = await request(app)
                .post('/v1/profile-address')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('X-Csrf-Token', csrfToken)
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('PUT запрос', async() => {
            const response = await request(app)
                .put('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('X-Csrf-Token', csrfToken)
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });

        test('DELETE запрос', async() => {
            const response = await request(app)
                .delete('/v1/profile-address/123')
                .send({ country: 'juniper' })
                .set('Host', 'api.tap.yandex.ru')
                .set('X-Request-Id', 'req-id')
                .set('X-Csrf-Token', csrfToken)
                .set('Cookie', 'Session_id=12345;yandexuid=431818718111111028')
                // Чтобы мидлвара cors выставила заголовки
                .set('Origin', 'http://localhost:8080')
                .expect('Access-Control-Allow-Credentials', 'true')
                .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
                .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
                .expect(500);

            expect(response.text).toEqual('Internal Server Error');
        });
    });

    test('Должен корректно обработать запрос типа OPTIONS', async() => {
        await request(app)
            .options(`/address/uid/${defaultBlackboxUid}/blue?source=${config.persAddress.source}`)
            .set('Origin', 'http://localhost:8080')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect('Access-Control-Max-Age', '86400')
            .expect(204);
    });
});
