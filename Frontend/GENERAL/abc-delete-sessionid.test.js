const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');

const DeleteSessionId = require('./abc-delete-sessionid');

describe('express/middlewares/abc-delete-sessionid', () => {
    const sessionCookies = ['Session_id', 'sessionid2'];
    const notSessionCookie = 'not_sessionid';

    const testCookies = [...sessionCookies, notSessionCookie].reduce(
        (acc, curr) => {
            acc[curr] = `test value (${curr})`;
            return acc;
        }, Object.create(null),
    );

    const app = express();

    app.use(cookieParser());
    app.use(DeleteSessionId.create());

    app.get('/', function(req, res) {
        Object.entries(testCookies).forEach(
            ([cookie, value]) => {
                res.cookie(cookie, value);
            },
        );
        res.send('ok');
    });

    app.get('/cookie', function(req, res) {
        expect(req.cookies[notSessionCookie]).toBe(testCookies[notSessionCookie]);
        for (let sessionCookie of sessionCookies) {
            expect(req.cookies[sessionCookie]).toBeUndefined();
        }
        res.send('ok');
    });

    const agent = supertest.agent(app);

    it('should accept cookies', function(done) {
        agent
            .get('/')
            .expect(res => {
                expect(res.headers['set-cookie']).toHaveLength(3);
            })
            .end(err => {
                done(err);
            });
    });

    it('should delete cookies', function(done) {
        agent
            .get('/cookie')
            .expect('ok')
            .end(err => {
                done(err);
            })
        ;
    });
});
