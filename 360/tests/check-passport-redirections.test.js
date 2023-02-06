const checkRedirections = require('../helpers/check-passport-redirections');

const retpath = 'https://disk.yandex.ru/test/path?a=1&b=2';
const encodedRetpath = 'https%3A%2F%2Fdisk.yandex.ru%2Ftest%2Fpath%3Fa%3D1%26b%3D2';

describe('check-passport-redirections', () => {
    it('user authed => no redirect', () => {
        const result = checkRedirections({ auth: true });
        expect(result).toBe('');
    });

    it('user not authed & allowed not auth => no redirect', () => {
        const result = checkRedirections({ auth: false }, { allowNotAuth: true });
        expect(result).toBe('');
    });

    it('user not authed & no retpath/origin => to auth', () => {
        const result = checkRedirections({ auth: false });
        expect(result).toBe('/auth?');
    });

    it('user needsReset', () => {
        const result = checkRedirections({ needsReset: true }, { origin: 'https://passport.yandex.ru', retpath });
        expect(result).toBe(`https://passport.yandex.ru/auth/update/?retpath=${encodedRetpath}`);
    });

    it('user needsUpgrade', () => {
        const result = checkRedirections({ needsUpgrade: true }, { origin: 'https://passport.yandex.ru', retpath });
        expect(result).toBe(`https://passport.yandex.ru/profile/upgrade?retpath=${encodedRetpath}`);
    });

    it('user needsPostregistration', () => {
        const result = checkRedirections(
            { needsPostregistration: true },
            {
                origin: 'https://passport.yandex.ru',
                retpath,
                extraQuery: { from: 'cloud' }
            }
        );
        expect(result).toBe(`https://passport.yandex.ru/passport?mode=postregistration&create_login=1&create_password=1&from=cloud&retpath=${encodedRetpath}`);
    });

    it('user needsUserApprove', () => {
        const result = checkRedirections(
            { needsUserApprove: true },
            {
                origin: 'https://passport.yandex.ru',
                retpath,
                extraQuery: { from: 'cloud' }
            }
        );
        expect(result).toBe(`https://passport.yandex.ru/passport?mode=userapprove&from=cloud&retpath=${encodedRetpath}`);
    });

    it('user error => to /auth', () => {
        const result = checkRedirections(
            { error: true },
            {
                origin: 'https://passport.yandex.ru',
                retpath,
                extraQuery: { from: 'cloud', origin: 'disk_public_ru' }
            }
        );
        expect(result).toBe(`https://passport.yandex.ru/auth?from=cloud&origin=disk_public_ru&retpath=${encodedRetpath}`);
    });

    it('user not authed & not allowed not auth => to /auth', () => {
        const result = checkRedirections(
            { auth: false },
            {
                allowNotAuth: false,
                origin: 'https://passport.yandex.ru',
                retpath,
                extraQuery: { from: 'cloud', origin: 'disk_public_ru' }
            }
        );
        expect(result).toBe(`https://passport.yandex.ru/auth?from=cloud&origin=disk_public_ru&retpath=${encodedRetpath}`);
    });
});
