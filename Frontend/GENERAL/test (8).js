/* global describe, it */

'use strict';

const proxyquire = require('proxyquire');
const expressHttpUatraits = require('./');

const HttpUatraitsStub = {
    HttpUatraits: function() {},
};

HttpUatraitsStub.HttpUatraits.prototype.detect = () => Promise.reject('Emulated bad response.');

const expressHttpUatraitsStub = proxyquire('./', { '@yandex-int/node-http-uatraits': HttpUatraitsStub });

describe('express-http-uatraits', () => {
    it('should just work', done => {
        const req = {
            headers: {
                'user-agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1',
            },
        };

        expressHttpUatraits()(req, null, () => {
            if (!req.uatraits) {
                return done(new Error('No `req.uatraits`'));
            }
            if (!req.uatraits.isBrowser) {
                return done(new Error('`req.uatraits` is wrong'));
            }
            return done();
        });
    });

    it('should give correct result without user-agent header', done => {
        const req = { headers: {} };

        expressHttpUatraits()(req, null, () => {
            if (!req.uatraits) {
                return done(new Error('No `req.uatraits`'));
            }
            if (Object.keys(req.uatraits).length !== 0) {
                return done(new Error('`req.uatraits` must be an empty object'));
            }
            return done();
        });
    });

    it('fallback should work', done => {
        const req = {
            headers: {
                'user-agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1',
            },
        };

        expressHttpUatraitsStub()(req, null, () => {
            if (!req.uatraits) {
                return done(new Error('No `req.uatraits`'));
            }

            if (!req.uatraits.isBrowser) {
                return done(new Error('`req.uatraits` is wrong'));
            }

            return done();
        });
    });

    it('fallback should work with empty user-agent', done => {
        const req = {
            headers: {},
        };

        expressHttpUatraitsStub()(req, null, () => {
            if (!req.uatraits) {
                return done(new Error('No `req.uatraits`'));
            }

            if (Object.keys(req.uatraits).length !== 0) {
                return done(new Error('`req.uatraits` must be an empty object'));
            }

            return done();
        });
    });

    it('detect error should set error field', done => {
        const req = {
            headers: {
                'user-agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1',
            },
        };

        expressHttpUatraitsStub()(req, null, () => {
            if (!req.uatraits.error) {
                return done(new Error('expect `req.uatraits.error`'));
            }

            return done();
        });
    });
});
