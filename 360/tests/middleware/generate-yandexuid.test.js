const getGenerateYandexuidMiddleware = require('../../middleware/generate-yandexuid');

describe('getGenerateYandexuidMiddleware', () => {
    it('должна вызывать next', () => {
        const next = jest.fn();
        getGenerateYandexuidMiddleware({})(
            { cookies: {}, ua: {} },
            { cookie: () => {} },
            next
        );
        expect(next).toBeCalled();
    });

    it('не должна генерировать yandexuid если он уже есть', () => {
        const req = { cookies: { yandexuid: '123' }, ua: {} };
        const cookie = jest.fn();
        getGenerateYandexuidMiddleware({})(
            req,
            { cookie },
            () => {}
        );
        expect(req.cookies.yandexuid).toBe('123');
        expect(cookie).toHaveBeenCalledTimes(0);
    });

    it('должна сгенерировать yandexuid если его нет и выставить куку на ru домен', () => {
        const req = { cookies: {}, ua: {}, tld: 'ru', fullTld: 'ru' };
        const cookie = jest.fn();
        const originalNow = Date.now;
        const originalRandom = Math.random;
        Date.now = jest.fn(() => 1522405223068);
        Math.random = jest.fn(() => 0.5);

        getGenerateYandexuidMiddleware({})(
            req,
            { cookie },
            () => {}
        );

        expect(req.cookies.yandexuid).toBe('5005000001522405223');
        expect(cookie).toHaveBeenCalledWith('yandexuid', '5005000001522405223', {
            maxAge: 315360000000,
            domain: '.yandex.ru',
            secure: true
        });
        Date.now = originalNow;
        Math.random = originalRandom;
    });

    it('должна сгенерировать yandexuid если его нет и выставить куку с `SameSite=None` на ru домен', () => {
        const req = { cookies: { }, tld: 'ru', fullTld: 'ru', ua: { SameSiteSupport: true } };
        const cookie = jest.fn();
        const originalNow = Date.now;
        const originalRandom = Math.random;
        Date.now = jest.fn(() => 1522405223068);
        Math.random = jest.fn(() => 0.5);

        getGenerateYandexuidMiddleware({})(
            req,
            { cookie },
            () => {}
        );

        expect(req.cookies.yandexuid).toBe('5005000001522405223');
        expect(cookie).toHaveBeenCalledWith('yandexuid', '5005000001522405223', {
            maxAge: 315360000000,
            domain: '.yandex.ru',
            sameSite: 'None',
            secure: true
        });
        Date.now = originalNow;
        Math.random = originalRandom;
    });

    it('должна сгенерировать yandexuid если его нет и выставить куку на sk', () => {
        const req = { cookies: {}, ua: {}, tld: 'sk' };
        const cookie = jest.fn();
        const originalNow = Date.now;
        const originalRandom = Math.random;
        Date.now = jest.fn(() => 1522405223068);
        Math.random = jest.fn(() => 0.5);

        getGenerateYandexuidMiddleware({})(
            req,
            { cookie },
            () => {}
        );

        expect(req.cookies.yandexuid).toBe('5005000001522405223');
        expect(cookie).toHaveBeenCalledWith('yandexuid', '5005000001522405223', {
            maxAge: 315360000000,
            domain: '.yadi.sk',
            secure: true
        });
        Date.now = originalNow;
        Math.random = originalRandom;
    });

    it('должна сгенерировать yandexuid если его нет и выставить куку с `SameSite=None` на sk', () => {
        const req = { cookies: { }, tld: 'sk', ua: { SameSiteSupport: true } };
        const cookie = jest.fn();
        const originalNow = Date.now;
        const originalRandom = Math.random;
        Date.now = jest.fn(() => 1522405223068);
        Math.random = jest.fn(() => 0.5);

        getGenerateYandexuidMiddleware({})(
            req,
            { cookie },
            () => {}
        );

        expect(req.cookies.yandexuid).toBe('5005000001522405223');
        expect(cookie).toHaveBeenCalledWith('yandexuid', '5005000001522405223', {
            maxAge: 315360000000,
            domain: '.yadi.sk',
            secure: true,
            sameSite: 'None'
        });
        Date.now = originalNow;
        Math.random = originalRandom;
    });
});
