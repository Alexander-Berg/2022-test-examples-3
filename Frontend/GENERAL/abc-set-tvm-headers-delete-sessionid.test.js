const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');

const AbcSetTvmHeadersDeleteSessionId = require('./abc-set-tvm-headers-delete-sessionid');
const AbcAddExpCookie = require('./abc-add-exp-cookie');

describe('express/middlewares/abc-set-tvm-headers-delete-sessionid', () => {
    const sessionCookies = ['Session_id', 'sessionid2'];
    const notSessionCookie = 'not_sessionid';

    const whiteListUrl = '/api/v3/resources/consumers/100500/actions/';
    const whiteListUrlDelete = '/api/v3/resources/consumers/100500/';

    const testCookies = [...sessionCookies, notSessionCookie].reduce(
        (acc, curr) => {
            acc[curr] = `test value (${curr})`;
            return acc;
        }, Object.create(null),
    );

    function expectCookieStays(req, res) {
        expect(req.cookies[notSessionCookie]).toBe(testCookies[notSessionCookie]);
        for (let sessionCookie of sessionCookies) {
            expect(req.cookies[sessionCookie]).toBe(testCookies[sessionCookie]);
        }
        res.send('ok');
    }

    function expectCookieDeleted(req, res) {
        expect(req.cookies[notSessionCookie]).toBe(testCookies[notSessionCookie]);
        for (let sessionCookie of sessionCookies) {
            expect(req.cookies[sessionCookie]).toBeUndefined();
        }
        res.send('ok');
    }

    const app = express();

    app.use(cookieParser());

    app.use(AbcAddExpCookie.create());
    app.use(AbcSetTvmHeadersDeleteSessionId.create());

    app.get('/', function(req, res) {
        Object.entries(testCookies).forEach(
            ([cookie, value]) => {
                res.cookie(cookie, value);
            },
        );
        res.send('ok');
    });

    app.get('/cookie-stays', expectCookieStays);
    app.get('/cookie-deleted', expectCookieDeleted);
    app.get(whiteListUrl, expectCookieStays);
    app.get(whiteListUrlDelete, expectCookieDeleted);
    app.delete(whiteListUrlDelete, expectCookieStays);

    const agent = supertest.agent(app);

    it('should delete session cookies', function(done) {
        agent
            .get('/')
            .end(() => {
                agent
                    .get('/cookie-deleted')
                    .expect('ok')
                    .end(err => {
                        done(err);
                    });
            });
    });

    it('should keep cookies for white list url', function(done) {
        agent
            .get('/')
            .end(() => {
                agent
                    .get(whiteListUrl)
                    .expect('ok')
                    .end(err => {
                        done(err);
                    });
            });
    });

    it('should delete session cookies for delete white list url if request method is not delete', function(done) {
        agent
            .get('/')
            .end(() => {
                agent
                    .get(whiteListUrlDelete)
                    .expect('ok')
                    .end(err => {
                        done(err);
                    });
            });
    });

    it('should keep cookies for delete white list url if request method is delete', function(done) {
        agent
            .get('/')
            .end(() => {
                agent
                    .delete(whiteListUrlDelete)
                    .expect('ok')
                    .end(err => {
                        done(err);
                    });
            });
    });
});
