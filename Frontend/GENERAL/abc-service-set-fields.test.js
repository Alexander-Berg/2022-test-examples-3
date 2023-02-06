const assert = require('assert');

const supertest = require('supertest');
const express = require('express');

describe('express/middlewares/abc-service-set-fields', () => {
    const AbcServiceSetFields = require('./abc-service-set-fields');

    it('Should add res.locals.service.fields', done => {
        const app = express();

        app.use((req, res, next) => {
            res.locals = {
                location: { query: { field: [
                    'foo',
                ] } },
            };
            next();
        });

        app.use(AbcServiceSetFields.create({ fields: [
            'available_states',
        ] }));

        app.get('/', (req, res) => {
            assert.deepEqual(res.locals.service.fields, [
                'available_states',
                'foo',
            ]);
            res.sendStatus(555);
        });

        supertest(app)
            .get('/?')
            .expect(555)
            .end(done);
    });
});
