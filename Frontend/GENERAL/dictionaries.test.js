const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

const TEST_DICTIONARIES = {
    THIS: 'IS_FINE',
};

const fakeLogger = (req, res, next) => {
    req.logger = {
        child() {
            return {
                info() {},
                debug() {},
            };
        },
    };
    next();
};

describe('express/dispenser/dictionaries', () => {
    it('Should set locals dictionaries from backend', done => {
        const Dictionaries = require('./dictionaries');

        fakeBack(back => {
            back.use(jsonParser());
            back.get('/common/api/front/dictionaries', (req, res) => {
                res.json(TEST_DICTIONARIES);
            });
        },
        (port, hostname, close) => {
            const app = express();

            app.use(cookieParser());
            app.use(jsonParser());
            app.use(fakeLogger);
            app.use((req, res, next) => {
                res.locals.sessionid = { login: 'usertest' };
                next();
            });

            app.use(Dictionaries.create({
                port,
                hostname,
            }));

            app.get('/', (req, res) => {
                assert.deepEqual(res.locals.dictionaries, TEST_DICTIONARIES);
                res.send(555);
            });

            supertest(app)
                .get('/')
                .expect(555)
                .end((...args) => {
                    close();
                    done(...args);
                });
        });
    });
});
