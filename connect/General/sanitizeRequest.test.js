const expect = require('chai').expect;
const sanitizeRequest = require('./sanitizeRequest');

const reqStringWithSlash = {
    method: 'get',
    url: 'http://pdd-back01g.dev.yandex.net/iapi/domain/details',
    noTrailingSlash: false,
};
const reqStringWithoutSlash = {
    method: 'get',
    url: 'http://pdd-back01g.dev.yandex.net/iapi/domain/details',
    noTrailingSlash: true,
};
const reqArrayWithSlash = {
    method: 'get',
    url: ['https://api-internal-test.directory.ws.yandex.net', 'v8',
        'organizations'],
    noTrailingSlash: false,
};
const reqArrayWithoutSlash = {
    method: 'get',
    url: ['https://api-internal-test.directory.ws.yandex.net', 'v8',
        'organizations'],
    noTrailingSlash: true,
};

describe('sanitizeRequest', () => {
    it('должен подставить "/" в конец строки (url - строка)', () => {
        expect(sanitizeRequest(reqStringWithSlash).url.endsWith('/'))
            .to.be.equal(true);
    });

    it('не должен подставлять в конец строки "/" (url - строка)', () => {
        expect(sanitizeRequest(reqStringWithoutSlash).url.endsWith('/'))
            .to.be.equal(false);
    });

    it('должен подставить "/" в конец строки (url - массив)', () => {
        expect(sanitizeRequest(reqArrayWithSlash).url.endsWith('/'))
            .to.be.equal(true);
    });

    it('не должен подставлять в конец строки "/" (url - массив)', () => {
        expect(sanitizeRequest(reqArrayWithoutSlash).url.endsWith('/'))
            .to.be.equal(false);
    });
});
