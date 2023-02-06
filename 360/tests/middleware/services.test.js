const servicesMiddleware = require('../../middleware/services');
const { DEFAULT_SERVICES_LIST, OUR_SERVICES_HASHMAP } = require('../../helpers/services');

describe('servicesMiddleware', () => {
    it('должна вызывать next', () => {
        const next = jest.fn();
        servicesMiddleware({})({ hostname: 'yadi.sk' }, {}, next);
        expect(next).toBeCalled();
    });

    it('список сервисов для прода Диска', () => {
        const req = {
            tld: 'ru',
            hostname: 'disk.yandex.ru'
        };
        servicesMiddleware({
            environment: 'production',
            servicesList: ['disk', 'www'],
            currentService: OUR_SERVICES_HASHMAP.CLIENT
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.yandex.ru', www: 'https://yandex.ru' });
    });

    it('список сервисов для престейбла Диска', () => {
        const req = {
            tld: 'ru',
            hostname: 'disk.dsp.yandex.ru'
        };
        servicesMiddleware({
            environment: 'prestable',
            servicesList: ['disk', 'www'],
            currentService: OUR_SERVICES_HASHMAP.CLIENT
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.dsp.yandex.ru', www: 'https://yandex.ru' });
    });

    it('список сервисов для прода паблика', () => {
        const req = {
            tld: 'ru',
            hostname: 'yadi.sk'
        };
        servicesMiddleware({
            environment: 'production',
            servicesList: ['disk', 'www'],
            currentService: OUR_SERVICES_HASHMAP.PUBLIC
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.yandex.ru', www: 'https://yandex.ru' });
    });

    it('список сервисов для престейбла паблика', () => {
        const req = {
            tld: 'ru',
            hostname: 'public.dsp.yadi.sk'
        };
        servicesMiddleware({
            environment: 'prestable',
            servicesList: ['disk', 'www'],
            currentService: OUR_SERVICES_HASHMAP.PUBLIC
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.dsp.yandex.ru', www: 'https://yandex.ru' });
    });

    it('список сервисов для прода DV', () => {
        const req = {
            tld: 'ru',
            hostname: 'docviewer.yandex.ru'
        };
        servicesMiddleware({
            environment: 'production',
            servicesList: ['disk', 'www'],
            currentService: OUR_SERVICES_HASHMAP.DOCVIEWER
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.yandex.ru', www: 'https://yandex.ru' });
    });

    it('список сервисов для престейбла DV', () => {
        const req = {
            tld: 'ru',
            hostname: 'docviewer.dsp.yandex.ru'
        };
        servicesMiddleware({
            environment: 'prestable',
            servicesList: ['disk', 'www'],
            currentService: OUR_SERVICES_HASHMAP.DOCVIEWER
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.yandex.ru', www: 'https://yandex.ru' });
    });

    it('tld из конфига должен быть важнее tld из req', () => {
        const req = {
            tld: 'sk',
            hostname: 'yadi.sk'
        };
        servicesMiddleware({
            tld: 'com',
            environment: 'production',
            servicesList: ['disk', 'www']
        })(req, {}, () => {});
        expect(req.yandexServices).toEqual({ disk: 'https://disk.yandex.com', www: 'https://yandex.com' });
    });

    it('список сервисов по умолчанию', () => {
        const req = {
            tld: 'ru',
            hostname: 'disk.yandex.ru'
        };
        servicesMiddleware({
            environment: 'production'
        })(req, {}, () => {});
        expect(Object.keys(req.yandexServices)).toEqual(DEFAULT_SERVICES_LIST);
    });
});
