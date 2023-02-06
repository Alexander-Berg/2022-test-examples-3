import expect from 'expect.js';
const originChecker = require('../../ufo-common/mail-origin-checker');

describe('Origin check (ufo-common/origin-checker.js)', () => {
    const getReq = () => ({
        headers: {
            origin: 'https://mail.yandex.ru'
        }
    });

    const isMailOrigin = originChecker;

    it('should allow origin', () => {
        expect(isMailOrigin(getReq()).allow).to.be(true);
    });

    it('should not allow origin', () => {
        const req = getReq();
        req.headers.origin = 'https://mail.yandex.com.evil';

        expect(isMailOrigin(req).allow).to.be(false);
    });

    it('should default to referer if origin is missing', () => {
        const req = getReq();
        req.headers.origin = undefined;
        req.headers.referer = 'https://mail.yandex.ru';

        expect(isMailOrigin(req).allow).to.be(true);
    });

    it('should return a hostname', () => {
        const req = getReq();

        expect(isMailOrigin(req).url).to.be(req.headers.origin);

        req.headers.origin = undefined;
        req.headers.referer = 'https://mail.yandex.ru/some/random/path?with=get&params=true';

        expect(isMailOrigin(req).url).to.equal('https://mail.yandex.ru');
    })
});
