const assert = require('assert');
const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/abc-configs', () => {
    const AbcConfigs = require('./abc-configs');

    it('Should copy params to res.locals.configs', done => {
        const app = express();
        const expect = {
            foo: 'bar',
        };

        app.use(AbcConfigs.create(expect));

        app.get('/', (req, res) => {
            assert.deepEqual(res.locals.configs, expect);
            res.sendStatus(555);
        });

        supertest(app)
            .get('/')
            .expect(555)
            .end(done);
    });
});
