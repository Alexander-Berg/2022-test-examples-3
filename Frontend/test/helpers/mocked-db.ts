import { ObjectId, MongoClient, Collection } from 'mongodb';

import DB from '../../src/lib/db';

export function createCollectionStub(fn: Function) {
    return {
        findOne: fn,
        insertOne: fn,
        updateOne: fn,
    } as Collection;
}

export function getMockedDBClass(collection: Collection) {
    return class MockedDB extends DB {
        constructor() {
            super('test', {
                host: 'test',
            });
        }

        createId() {
            return 'fake-id' as unknown as ObjectId;
        }

        protected _buildMongoClient() {
            return {
                async connect() {},
                async disconnect() {},
                async createCollection() {},

                db() {
                    return {
                        collection() {
                            return collection;
                        },
                    };
                },
            } as unknown as MongoClient;
        }
    };
}
