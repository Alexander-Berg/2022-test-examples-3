const assert = require('assert');
const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const jsonParser = require('body-parser').json;
const fakeBack = require('../../../tools/fake-back');

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

describe('express/dispenser/ask-permissions', () => {
    it('Should fetch dispenser permissions from backend', done => {
        const DispenserPermissions = require('./ask-permissions');

        fakeBack(
            back => {
                back.use(jsonParser());
                back.get('/common/api/v1/permission', (req, res) => {
                    res.json({
                        login: 'john.doe',
                        permissionSet: [
                            'FOO',
                            'BAR',
                        ],
                    });
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

                app.use(DispenserPermissions.create({
                    port,
                    hostname,
                }));

                app.get('/', (req, res) => {
                    assert.deepEqual(res.locals.dispenserPermissions, ['FOO', 'BAR']);
                    res.send(555);
                });

                supertest(app)
                    .get('/')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });
});
