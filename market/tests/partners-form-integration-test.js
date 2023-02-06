'use strict';

/* global describe, it, afterEach, beforeEach, require */
const request = require('supertest');
const { expect } = require('chai');
const { Validator } = require('jsonschema');
const middlewareMock = require('./helper/middleware-mock');

describe('Partners request form', () => {
    let app;

    const v = new Validator();

    const Form = {
        type: 'object',
        properties: {
            step1: {
                type: 'array',
                minItems: 1
            },
            step2: {
                type: 'object',
                properties: {
                    products: { type: 'array', minItems: 1 },
                    contract: { type: 'array', minItems: 1 }
                }
            }
        }
    };

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    it('should return form from bunker', done => {
        request(app)
            .get('/adv/partners/request')
            .set('host', 'yandex.ru')
            .expect(200)
            .end((err, data) => {
                expect(v.validate(data.body.form, Form).valid).to.equal(true);
                done(err);
            });
    });

    afterEach(middlewareMock.integrationAfter);
});
