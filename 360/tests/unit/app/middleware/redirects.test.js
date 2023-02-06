const getRedirectsMiddleware = require('../../../../app/middleware/redirects');

describe('redirectsMiddleware', () => {
    let res;
    beforeEach(() => {
        res = {
            redirect(url) {
                res.location = url;
            },
            send(data) {
                res.response = data;
            }
        };
    });

    it('не должна редиректить с disk.yandex.ru/public/ на yadi.sk/public/', () => {
        const req = {
            tld: 'ru',
            originalUrl: '/public/',
            hostname: 'disk.yandex.ru',
            ua: {}
        };
        const next = jest.fn();
        getRedirectsMiddleware()(req, res, next);
        expect(next).toBeCalled();
    });

    it('не должна редиректить с disk.yandex.ru/public/nda/ на yadi.sk/public/nda/', () => {
        const req = {
            tld: 'ru',
            originalUrl: '/public/nda/',
            hostname: 'disk.yandex.ru',
            ua: {}
        };
        const next = jest.fn();
        getRedirectsMiddleware()(req, res, next);
        expect(next).toBeCalled();
    });

    it('должна отдавать скрипт с редиректом для disk.yandex.net/disk/public/', () => {
        const req = {
            tld: 'net',
            originalUrl: '/disk/public/',
            hostname: 'disk.yandex.net',
            ua: {}
        };
        getRedirectsMiddleware()(req, res);
        expect(res.response).toBe(
            '<script>location="https://yadi.sk/public/?hash="+location.hash.replace(/^#/,\"\")</script>'
        );
    });

    it('должна редиректить с disk.yandex.net/disk/public/?hash=hash на yadi.sk/public/?hash=hash', () => {
        const req = {
            tld: 'net',
            query: {
                hash: 'hash'
            },
            originalUrl: '/disk/public/?hash=hash',
            hostname: 'disk.yandex.net',
            ua: {}
        };
        getRedirectsMiddleware()(req, res);
        expect(res.location).toBe('https://yadi.sk/public/?hash=hash');
    });

    it('должна редиректить с yadi.sk на disk.yandex.tld в mobile Safari 12 если есть кука authTld', () => {
        const req = {
            tld: 'sk',
            originalUrl: '/d/hash',
            hostname: 'yadi.sk',
            cookies: {
                authTld: 'com'
            },
            ua: {
                isMobile: true,
                BrowserName: 'MobileSafari',
                BrowserVersion: '12.1'
            }
        };
        getRedirectsMiddleware()(req, res);
        expect(res.location).toBe('https://disk.yandex.com/d/hash');
    });

    it('должна редиректить с yadi.sk на disk.yandex.tld в Safari 12 если есть кука tld', () => {
        const req = {
            tld: 'sk',
            originalUrl: '/d/hash',
            hostname: 'yadi.sk',
            cookies: {
                tld: 'com'
            },
            ua: {
                isMobile: false,
                BrowserName: 'Safari',
                BrowserVersion: '12.1'
            }
        };
        getRedirectsMiddleware()(req, res);
        expect(res.location).toBe('https://disk.yandex.com/d/hash');
    });

    it('должна редиректить с yadi.sk на геодомен в safari 12 если есть нет куки tld', () => {
        const req = {
            tld: 'sk',
            originalUrl: '/d/hash',
            hostname: 'yadi.sk',
            cookies: {},
            countryInfo: {
                iso_name: 'tr'
            },
            ua: {
                isMobile: false,
                BrowserName: 'Safari',
                BrowserVersion: '12.1'
            }
        };
        getRedirectsMiddleware()(req, res);
        expect(res.location).toBe('https://disk.yandex.com.tr/d/hash');
    });
});
