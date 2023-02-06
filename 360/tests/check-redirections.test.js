const checkRedirections = require('../helpers/check-redirections');
jest.mock('@ps-int/ufo-server-side-commons/config', () => {
    return {
        defaultDomain: 'com',
        domains: {
            ru: {
                domain: 'ru'
            }
        }
    };
});

jest.mock('../helpers/check-passport-redirections');
const checkPassportRedirections = require('../helpers/check-passport-redirections');

describe('check-redirections', () => {
    let req = {};

    beforeEach(() => {
        req = {
            hostname: 'disk.yandex.ru',
            originalUrl: '/test/path?a=1&b=2',
            fullTld: 'ru',
            countryInfo: {
                iso_name: 'ru'
            },
            yandexServices: {
                passport: 'https://passport.yandex.ru'
            },
            user: { name: 'Ivan', auth: true },
            diskSource: 'public',
            lang: 'ru'
        };
    });

    afterEach(() => {
        checkPassportRedirections.mockReset();
    });

    it('user authed => no redirect', () => {
        const result = checkRedirections(req);
        expect(result).toBe(false);
    });

    it('user not authed & allowed not auth => no redirect', () => {
        req.user.auth = false;

        const result = checkRedirections(req, { allowNotAuth: true });
        expect(result).toBe(false);
    });

    it('.net / known country => redirect to real TLD', () => {
        req.fullTld = 'net';
        req.hostname = 'disk.yandex.net';

        const result = checkRedirections(req);
        expect(result).toBe('https://disk.yandex.ru/test/path?a=1&b=2');
    });

    it('.net / unknown country => redirect to default domain', () => {
        req.fullTld = 'net';
        req.hostname = 'disk.yandex.net';
        req.countryInfo.iso_name = 'be';

        const result = checkRedirections(req);
        expect(result).toBe('https://disk.yandex.com/test/path?a=1&b=2');
    });

    it('.com and TR => redirect to .com.tr', () => {
        req.fullTld = 'com';
        req.countryInfo.iso_name = 'tr';
        req.hostname = 'disk.yandex.com';

        const result = checkRedirections(req);
        expect(result).toBe('https://disk.yandex.com.tr/test/path?a=1&b=2');
    });

    it('check passport redirect', () => {
        req.user.auth = false;
        checkPassportRedirections.mockImplementation(() => 'http://another.com?a=test');

        const result = checkRedirections(req);
        expect(result).toBe('http://another.com?a=test');
    });

    it('check passport redirect / no disk source', () => {
        delete req.diskSource;
        checkPassportRedirections.mockImplementation(() => 'http://another.com?a=test');

        const result = checkRedirections(req, { allowNotAuth: true });
        expect(result).toBe('http://another.com?a=test');

        expect(checkPassportRedirections).toHaveBeenCalledWith(
            {
                auth: true,
                name: 'Ivan'
            },
            {
                allowNotAuth: true,
                extraQuery: { from: 'cloud', origin: 'disk_client_web_signin_ru' },
                origin: 'https://passport.yandex.ru',
                retpath: 'https://disk.yandex.ru/test/path?a=1&b=2'
            }
        );
    })
});
