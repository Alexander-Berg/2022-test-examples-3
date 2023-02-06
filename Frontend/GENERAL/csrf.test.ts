import request from 'supertest';
import { advanceTo, clear as clearDateMocks } from 'jest-date-mock';

import app from '../app';
import { logger } from '../lib/logger';
import { deepFreeze, mockNetwork } from '../utils/tests';

describe('Router. csrf', () => {
    const networkMock = mockNetwork();

    const uatraitsData = deepFreeze({
        localStorageSupport: true,
        isTouch: false,
        BrowserEngine: 'Gecko',
        OSFamily: 'MacOS',
        BrowserEngineVersion: '42.0',
        BrowserVersion: '42.0',
        BrowserName: 'Firefox',
        CSP1Support: true,
        SVGSupport: true,
        historySupport: true,
        postMessageSupport: true,
        isBrowser: true,
        isMobile: false,
    });
    const mockYandexuid = () => {
        advanceTo(new Date(2020, 10, 27, 0, 0, 0));

        jest.spyOn(Math, 'random')
            .mockReturnValue(0.9969743934707453);
    };

    afterEach(() => {
        expect(networkMock.getLog()).toMatchSnapshot();

        clearDateMocks();
    });

    test('Должен создать csrf токен на основе yandexuid', async() => {
        networkMock.mockUaTraits({ data: uatraitsData });

        // Делаем генерацию yandexuid статичной
        mockYandexuid();

        await request(app)
            .get('/csrf')
            // Чтобы мидлвара cors выставила заголовки
            .set('Origin', 'http://localhost:8080')
            // user-agent нужен для запроса в uatraits
            .set('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0')
            // Яндексовый host нужен для мидлвары express-yandexuid, которая выставляет куку yandexuid, если её нет
            .set('Host', 'api.tap.yandex.ru')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect('set-cookie', 'yandexuid=9969743931606417200; Domain=.yandex.ru; Path=/; Expires=Tue, 26 Nov 2030 19:00:00 GMT')
            .expect(200, {
                value: 'e764b9b2e09dcd3e272f08a6f6fbdd926c587637:1606417200',
                ttl: 86400,
            });
    });

    test('Должен создать csrf токен на основе yandexuid, даже если запрос в uatraits упал', async() => {
        networkMock.mockUaTraits({ data: uatraitsData, statusCode: 500 });

        // Делаем генерацию yandexuid статичной
        mockYandexuid();

        await request(app)
            .get('/csrf')
            // Чтобы мидлвара cors выставила заголовки
            .set('Origin', 'http://localhost:8080')
            // user-agent нужен для запроса в uatraits
            .set('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0')
            // Яндексовый host нужен для мидлвары express-yandexuid, которая выставляет куку yandexuid, если её нет
            .set('Host', 'api.tap.yandex.ru')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect('set-cookie', 'yandexuid=9969743931606417200; Domain=.yandex.ru; Path=/; Expires=Tue, 26 Nov 2030 19:00:00 GMT')
            .expect(200, {
                value: 'e764b9b2e09dcd3e272f08a6f6fbdd926c587637:1606417200',
                ttl: 86400,
            });
    });

    test('Должен создать csrf токен на основе уже имеющейся куки yandexuid', async() => {
        networkMock.mockUaTraits({ data: uatraitsData });

        // Мокаем дату для статичной генерации csrf-токена
        advanceTo(new Date(2020, 10, 28, 0, 0, 0));

        await request(app)
            .get('/csrf')
            // Чтобы мидлвара cors выставила заголовки
            .set('Origin', 'http://localhost:8080')
            // user-agent нужен для запроса в uatraits
            .set('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0')
            // Яндексовый host нужен для мидлвары express-yandexuid, которая выставляет куку yandexuid, если её нет
            .set('Host', 'api.tap.yandex.ru')
            .set('Cookie', 'yandexuid=9969743931606417200')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect(res => expect(res.headers).not.toHaveProperty('set-cookie'))
            .expect(200, {
                value: '8e009629a3cce71d4d3259d51d3096f4f738f3ab:1606503600',
                ttl: 86400,
            });
    });

    test('Должен создать csrf токен на основе уже имеющейся куки yandexuid, даже если запрос в uatraits упал', async() => {
        networkMock.mockUaTraits({ data: uatraitsData, statusCode: 500 });

        // Мокаем дату для статичной генерации csrf-токена
        advanceTo(new Date(2020, 10, 29, 0, 0, 0));

        await request(app)
            .get('/csrf')
            // Чтобы мидлвара cors выставила заголовки
            .set('Origin', 'http://localhost:8080')
            // user-agent нужен для запроса в uatraits
            .set('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0')
            // Яндексовый host нужен для мидлвары express-yandexuid, которая выставляет куку yandexuid, если её нет
            .set('Host', 'api.tap.yandex.ru')
            .set('Cookie', 'yandexuid=9969743931606417200')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect(res => expect(res.headers).not.toHaveProperty('set-cookie'))
            .expect(200, {
                value: '9cecc07c55ae7044f425b161e1d067a1f49e1807:1606590000',
                ttl: 86400,
            });
    });

    test('Должен выбросить ошибку, если мидлвара express-yandexuid не выставила куку yandexuid', async() => {
        const spyLogger = jest.spyOn(logger, 'error');

        await request(app)
            .get('/csrf')
            // Чтобы мидлвара cors выставила заголовки
            .set('Origin', 'http://localhost:8080')
            // Не указали user-agent, чтобы не было запроса в uatraits
            // Не имея данных из uatraits кука yandexuid выставлена не будет
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect(500, {
                name: 'InternalServerError',
                status: 500,
                message: 'Missing yandexuid in cookies',
            });

        expect(spyLogger).toHaveBeenCalledTimes(1);
    });

    test('Должен корректно обработать запрос типа OPTIONS', async() => {
        await request(app)
            .options('/csrf')
            .set('Origin', 'http://localhost:8080')
            .expect('Access-Control-Allow-Credentials', 'true')
            .expect('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE')
            .expect('Access-Control-Allow-Origin', 'http://localhost:8080')
            .expect('Access-Control-Expose-Headers', 'X-Csrf-Token,X-Request-Id')
            .expect('Access-Control-Max-Age', '86400')
            .expect(204);
    });
});
