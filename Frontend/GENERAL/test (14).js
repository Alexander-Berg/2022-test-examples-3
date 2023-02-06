/* global describe, it */
const assert = require('assert');
const sinon = require('sinon');
const catchError = require('catch-error-async');

const config = {
    hostname: 'test-hostname.yandex-team.ru',
    issuer: [
        'DC=ru',
        'DC=yandex',
        'DC=ld',
        'CN=YandexInternalCA',
    ],
    subject: [
        'C=RU',
        'ST=Moscow',
        'L=Moscow',
        'O=Yandex LLC',
        'OU=ITO',
        'CN=idm.test.yandex-team.ru',
        'emailAddress=pki@yandex-team.ru',
    ],
};
const middlewareIdmAccess = require('.')(config);

describe('IDM access middleware', () => {
    it('should call `next` in success case', async() => {
        const ctx = {
            hostname: 'test-hostname.yandex-team.ru',
            headers: {
                'x-qloud-ssl-issuer': 'CN=YandexInternalCA,DC=ld,DC=yandex,DC=ru',
                'x-qloud-ssl-subject': 'emailAddress=pki@yandex-team.ru,CN=idm.test.yandex-team.ru,' +
                'OU=ITO,O=Yandex LLC,L=Moscow,ST=Moscow,C=RU',
            },
        };
        const spy = sinon.spy();

        await middlewareIdmAccess(ctx, spy);

        assert.ok(spy.calledOnce);
    });

    it('should throw 403 when hostname is invalid', async() => {
        const ctx = { hostname: 'wrong.hostname.ru' };
        const error = await catchError(middlewareIdmAccess, ctx);

        assert.equal(error.status, 403);
        assert.equal(error.message, `Application should locate on ${config.hostname}`);
        assert.deepEqual(error.options, { hostname: 'wrong.hostname.ru' });
    });

    it('should throw 403 when ssl issuer is not correct', async() => {
        const ctx = {
            hostname: 'test-hostname.yandex-team.ru',
            headers: { 'x-qloud-ssl-issuer': 'invalid ssl issuer' },
        };
        const error = await catchError(middlewareIdmAccess, ctx);

        assert.equal(error.status, 403);
        assert.equal(error.message, 'Invalid certificate data');
        assert.deepEqual(error.options, {
            isIssuerCorrect: false,
            isSubjectCorrect: false,
        });
    });

    it('should throw 403 when ssl subject is not correct', async() => {
        const ctx = {
            hostname: 'test-hostname.yandex-team.ru',
            headers: {
                'x-qloud-ssl-issuer': 'CN=YandexInternalCA,DC=ld,DC=yandex,DC=ru',
                'x-qloud-ssl-subject': 'invalid ssl subject',
            },
        };
        const error = await catchError(middlewareIdmAccess, ctx);

        assert.equal(error.status, 403);
        assert.equal(error.message, 'Invalid certificate data');
        assert.deepEqual(error.options, {
            isIssuerCorrect: true,
            isSubjectCorrect: false,
        });
    });
});
