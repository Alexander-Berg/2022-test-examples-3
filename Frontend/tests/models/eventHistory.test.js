const assert = require('assert');
const catchErrorAsync = require('catch-error-async');
const _ = require('lodash');

const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const db = require('db');
const EventHistory = require('models/eventHistory');

const testDbType = DbType.internal;

describe('Event history model', () => {
    beforeEach(cleanDb);

    describe('dumpEntity', () => {
        it('should return data of entity using "history" scope', async() => {
            const data = { id: 8, slug: 'codefest', title: 'codefest' };

            await factory.event.create(data);

            const actual = await EventHistory.dumpEntity({ entityId: 8, dbType: testDbType });
            const expected = {
                ...factory.event.defaultData,
                ...data,
                programItems: [],
                sections: [],
                tags: [],
                groups: [],
                locations: [],
                broadcasts: [],
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw error if entityType is wrong', async() => {
            const error = await catchErrorAsync(
                EventHistory.dumpEntity.bind(EventHistory, { entityType: 'wrong', dbType: testDbType }),
            );

            assert.equal(
                error.message,
                'History.getTableByType: Table wrong is not existed',
            );
        });
    });

    describe('create', () => {
        it('should create history record', async() => {
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const history = new EventHistory({ entityId: 8, authorLogin: 'art00' }, { dbType: testDbType });

            await history.create(EventHistory.operationTypes.create, { dbType: testDbType });

            const historyRecords = await db.history.findAll();

            assert.equal(historyRecords.length, 1);
            assert.equal(historyRecords[0].entityId, 8);
            assert.equal(historyRecords[0].authorLogin, 'art00');
            assert.equal(historyRecords[0].operation, 'create');
            assert.equal(historyRecords[0].entityType, 'event');
        });

        it('should throw error if updating and previous data is not passed', async() => {
            const eventHistory = new EventHistory({ authorLogin: 'art00' }, { dbType: testDbType });
            const error = await catchErrorAsync(
                eventHistory.create.bind(eventHistory, EventHistory.operationTypes.update, { dbType: testDbType }),
            );

            assert.equal(
                error.message,
                'History.create: Previous data is required while updating',
            );
        });
    });

    describe('findPage', () => {
        it('should find history only for appropriate event', async() => {
            const entries = _.range(1, 26)
                .map(num => ({
                    id: num,
                    entityId: num > 10 ? { id: 144 } : { id: 152, slug: 'wrong' },
                    data: { mark: `#${num}` },
                }));

            await factory.eventHistory.create(entries);

            const actual = await EventHistory.findPage(144, {
                pageSize: 30,
                pageNumber: 1,
                dbType: testDbType,
            });

            assert.equal(actual.rows.length, 15);
            assert.equal(actual.rows[0].data.mark, '#25');
            assert.equal(actual.meta.totalSize, 15);
            assert.equal(actual.meta.pageNumber, 1);
            assert.equal(actual.meta.pageSize, 30);
        });
    });
});
