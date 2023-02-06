const assert = require('assert');
const sinon = require('sinon');

const db = require('db');
const dbChecker = require('models/heartbeat/check/db');
const cleanDb = require('tests/db/clean');

describe('DB checker', () => {
    beforeEach(async() => {
        await cleanDb();

        this.clock = sinon.useFakeTimers(123);
    });

    afterEach(() => {
        this.clock.restore();
    });

    it('should add new record', async() => {
        await dbChecker();

        const actual = await db.heartbeat.findAll({ raw: true });

        assert.equal(actual.length, 1);
        assert.deepEqual(actual[0], { id: 1, timestamp: 123 });
    });

    it('should update existing record', async() => {
        await db.heartbeat.create({ id: 1, timestamp: 12 });
        await dbChecker();

        const actual = await db.heartbeat.findAll({ raw: true });

        assert.equal(actual.length, 1);
        assert.deepEqual(actual[0], { id: 1, timestamp: 123 });
    });
});
