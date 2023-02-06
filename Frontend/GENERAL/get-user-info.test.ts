import request from 'supertest';

import app from '../app';
import { mockNetwork } from '../utils/tests';
import { logger } from '../lib/logger';

describe('Controller. get-user-info', () => {
    const networkMock = mockNetwork();
    let spyLoggerWarn: jest.SpyInstance;
    let spyLoggerError: jest.SpyInstance;

    const userInfoRequest = () => {
        return request(app)
            .get('/v1/auth/user-info')
            .set('X-Request-Id', 'req-id')
            .set('Origin', 'http://localhost:8080')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id');
    };

    beforeEach(() => {
        spyLoggerWarn = jest.spyOn(logger, 'warn');
        spyLoggerError = jest.spyOn(logger, 'error');
    });

    afterEach(() => {
        expect(networkMock.getLog()).toMatchSnapshot();
    });

    test('Должен вернуть полную информацию о пользователе', async function() {
        networkMock.mockTvm();
        networkMock.mockBlackbox({ isValid: true });

        await userInfoRequest()
            .set('Cookie', 'Session_id=12345;')
            .expect(200, {
                uid: '123',
                portalUrl: 'https://yandex.ru',
                login: 'test-user',
                displayName: 'Test User',
                avatarId: '1/2-3',
                firstName: '',
                lastName: '',
                email: '',
                hasPlus: false,
            });
    });

    test('Должен вернуть минимальную информацию о пользователе', async function() {
        networkMock.mockTvm();
        networkMock.mockBlackbox({
            isValid: true,
            user: {
                login: undefined,
                displayName: undefined,
                avatarId: undefined,
            },
        });

        await userInfoRequest()
            .set('Cookie', 'Session_id=12345;')
            .expect(200, {
                uid: '123',
                portalUrl: 'https://yandex.ru',
                login: '',
                displayName: '',
                avatarId: '0/0-0',
                firstName: '',
                lastName: '',
                email: '',
                hasPlus: false,
            });
    });

    test('Должен вернуть 401 при отсутствии Session_id куки', async function() {
        networkMock.mockTvm();
        networkMock.mockBlackbox({ isValid: true });

        await userInfoRequest().expect(401, {
            name: 'UnauthorizedError',
            status: 401,
            message: 'Unauthorized',
        });

        expect(spyLoggerWarn).toHaveBeenCalledTimes(1);
    });

    test('Должен вернуть 401 при ответе паспорта о невалидной авторизации', async function() {
        networkMock.mockTvm();
        networkMock.mockBlackbox({ isValid: false });

        await userInfoRequest()
            .set('Cookie', 'Session_id=12345')
            .expect(401, {
                name: 'UnauthorizedError',
                status: 401,
                message: 'Unauthorized',
            });
    });

    test('Должен вернуть 500 при падении паспорта', async function() {
        networkMock.mockTvm();
        networkMock.mockBlackbox({ isValid: false, fatal: true });

        await userInfoRequest()
            .set('Cookie', 'Session_id=12345;')
            .expect(500, 'Internal Server Error');

        expect(spyLoggerError).toHaveBeenCalledTimes(1);
    });

    test('Должен падать при отсутствии тикета TVM для blackbox', async function() {
        networkMock.mockTvm({ blackboxErr: true });
        networkMock.mockBlackbox({ isValid: true });

        await userInfoRequest()
            .set('Cookie', 'Session_id=12345')
            .expect(500, 'Internal Server Error');

        expect(spyLoggerError).toHaveBeenCalledTimes(1);
    });

    test('Должен корректно обработать запрос типа OPTIONS', async function() {
        await request(app)
            .options('/auth/user-info')
            .set('X-Request-Id', 'req-id')
            .set('Origin', 'http://localhost:8080')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE')
            .expect('Access-Control-Max-Age', '86400')
            .expect(204);
    });
});
