import DB from '../../src/lib/db';

const TEST_DB_NAME = 'test-db';
const TEST_DB_HOST = 'localhost:27017';
const MONGO_CLIENT_OPTIONS = {
    replicaSet: 'local',
    authSource: TEST_DB_NAME,
};
const MONGO_CONNECTION_CONFIG = {
    host: TEST_DB_HOST,
};

export class TestDB extends DB {
    async flushDB() {
        await this._db.dropDatabase();
    }
}

export default function createTestDb() {
    return new TestDB(TEST_DB_NAME, MONGO_CONNECTION_CONFIG, MONGO_CLIENT_OPTIONS);
}
