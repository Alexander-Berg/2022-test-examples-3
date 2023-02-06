const assertEqual = require('assert').strictEqual;
const catchError = require('catch-error-async');
const uuid = require('uuid/v1');

const assert = require('lib/assert');

describe('Assert library overload', () => {
    describe('bySchema', () => {
        it('should do nothing when value is valid', () => {
            const eventData = {
                id: 13,
                slug: 'devstart',
                startDate: '2018-02-16T04:00:00.000Z',
                endDate: '2018-02-17T04:00:00.000Z',
                title: 'Starting this',
            };

            assert.bySchema(eventData, 'event');
        });

        it('should throw error when value is invalid', async() => {
            const eventData = {
                slug: 123,
                startDate: 'abc',
                registrationStatus: 'nonexisting',
                endDate: '2018-02-17T04:00:00.000Z',
                title: null,
            };
            const error = await catchError(assert.bySchema, eventData, 'event');

            assertEqual(error.message, 'Check by schema failed');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_CSF');
            assertEqual(error.options.errors.length, 4);
            assertEqual(error.options.errors[0].message,
                'should be string');
            assertEqual(error.options.errors[1].message,
                'should match format "date-time"');
            assertEqual(error.options.errors[2].message,
                'should be equal to one of the allowed values');
            assertEqual(error.options.errors[3].message,
                'should be string');
        });
    });

    describe('byPartialSchema', () => {
        it('should do nothing when value is valid', () => {
            const eventData = {
                id: 11,
                slug: 'devstart',
            };

            assert.byPartialSchema(eventData, 'event');
        });

        it('should throw error when value is invalid', async() => {
            const eventData = {
                id: 11,
                title: null,
            };
            const error = await catchError(assert.byPartialSchema, eventData, 'event');

            assertEqual(error.message, 'Check by schema failed');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_CSF');
            assertEqual(error.options.errors.length, 1);
            assertEqual(error.options.errors[0].message,
                'should be string');
        });
    });

    describe('bySchemaFromQuery', () => {
        it('should do nothing when value is valid', () => {
            assert.bySchemaFromQuery('1', { type: 'number' });
            assert.bySchemaFromQuery('true', { type: 'boolean' });
            assert.bySchemaFromQuery(
                '2018-10-26T12:00:00.000Z', { type: 'string', format: 'date-time' },
            );
        });

        it('should throw error when number is invalid', async() => {
            const error = await catchError(assert.bySchemaFromQuery, 'invalid', { type: 'number' });

            assertEqual(error.message, 'Float is invalid');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_FVI');
        });

        it('should throw error when boolean is invalid', async() => {
            const error = await catchError(
                assert.bySchemaFromQuery, 'invalid', { type: 'boolean' },
            );

            assertEqual(error.message, 'Check by schema failed');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_CSF');
        });

        it('should throw error when date is invalid', async() => {
            const error = await catchError(
                assert.bySchemaFromQuery, 'invalid', { type: 'string', format: 'date-time' },
            );

            assertEqual(error.message, 'Check by schema failed');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_CSF');
        });
    });

    describe('email', () => {
        it('should do nothing when value is correct email', () => {
            assert.email('saaaaaaaaasha@yandex-team.ru');
            assert.email('art0.0@yandex.ru');
        });

        it('should throw error when value is invalid email', async() => {
            const error = await catchError(assert.email, 'invalid@@email.ru');

            assertEqual(error.message, 'Email is invalid');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_EII');
            assertEqual(error.options.value, 'invalid@@email.ru');
        });

        it('should throw error when value not passed', async() => {
            const error = await catchError(assert.email);

            assertEqual(error.message, 'Email is invalid');
        });
    });

    describe('tryEmail', () => {
        it('should do nothing when value is correct email', () => {
            assert.email('saaaaaaaaasha@yandex-team.ru');
            assert.email('art0.0@yandex.ru');
        });

        it('should throw error when value is invalid email', async() => {
            const error = await catchError(assert.email, 'invalid@@email.ru');

            assertEqual(error.message, 'Email is invalid');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_EII');
            assertEqual(error.options.value, 'invalid@@email.ru');
        });

        it('should do nothing when value not passed', () => {
            assert.tryEmail();
        });
    });

    describe('uuid', () => {
        it('should do nothing when value is correct uuid', () => {
            assert.uuid(uuid());
        });

        it('should throw error when value is invalid uuid', async() => {
            const error = await catchError(assert.uuid, 'an-invalid-uuid');

            assertEqual(error.message, 'secretKey has invalid uuid format');
            assertEqual(error.statusCode, 400);
            assertEqual(error.options.internalCode, '400_UIF');
            assertEqual(error.options.value, 'an-invalid-uuid');
        });
    });
});
