const assert = require('assert');
const catchErrorAsync = require('catch-error-async');
const config = require('yandex-cfg');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const Distribution = require('models/distribution');

const testDbType = DbType.internal;

describe('Distribution model', () => {
    beforeEach(require('tests/db/clean'));

    describe('findOne', () => {
        it('should find a distribution', async() => {
            const data = { id: 11, eventId: { id: 42 } };

            await factory.distribution.create(data);
            await factory.accountMail.create({ distributionId: 11, wasSent: true });

            const distribution = await Distribution.findOne({ id: 11, dbType: testDbType });
            const actual = distribution.toJSON();

            assert.equal(actual.id, data.id);
            assert.equal(actual.eventId, data.eventId.id);
            assert.equal(actual.sentCount, 1);
        });

        it('should throw if account not found', async() => {
            const error = await catchErrorAsync(
                Distribution.findOne.bind(Distribution), { id: 11, dbType: testDbType },
            );

            assert.equal(error.message, 'Distribution not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                dbType: testDbType,
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('create', () => {
        it('should save a new distribution', async() => {
            await factory.event.create({ id: 42, slug: 'devstart' });
            await factory.mailTemplate.create({ id: 11, name: 'test', externalSlug: 'TEST-TEST' });

            const data = {
                id: 5,
                eventId: 42,
                templateId: 11,
                filters: { invitationStatus: ['invite'] },
            };

            const item = new Distribution(data, { dbType: testDbType });
            const id = await item.create({ login: 'art00', dbType: testDbType });

            const actual = await db.distribution.findById(id);

            assert.equal(actual.status, config.schema.distributionStatusEnum.new);
            assert.equal(actual.byPreviousDistributionType, config.schema.distributionSendToEnum.all);
            assert.deepEqual(actual.filters, data.filters);
        });
    });

    describe('patch', () => {
        it('should patch an existing distribution', async() => {
            await factory.distribution.create({ id: 5 });

            const distribution = new Distribution({
                id: 5,
                status: config.schema.distributionStatusEnum.inProgress,
            }, { dbType: testDbType });
            const id = await distribution.patch({ dbType: testDbType });
            const actual = await db.distribution.findOne({ where: { id } });

            assert.equal(actual.id, id);
            assert.equal(actual.status, config.schema.distributionStatusEnum.inProgress);
        });

        it('should throw on nonexistent distribution', async() => {
            const distribution = new Distribution({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(distribution.patch.bind(distribution));

            assert.equal(error.message, 'Distribution not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('delete', () => {
        it('should delete an existing distribution', async() => {
            const id = 5;

            await factory.distribution.create({ id });

            await Distribution.destroy(id, { dbType: testDbType });

            const actual = await db.distribution.findOne({ where: { id } });

            assert.equal(actual, null);
        });

        it('should throw on nonexistent distribution', async() => {
            const error = await catchErrorAsync(Distribution.destroy.bind(Distribution, 13, { dbType: testDbType }));

            assert.equal(error.message, 'Distribution not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });
});
