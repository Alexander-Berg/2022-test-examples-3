import { openDatabase, removeCacheApiPromise, processRecord } from '../lib/datasync';
jest.mock('load-script');

describe('datasync ', () => {
    it('should return promise', () => {
        expect(
            openDatabase({
                apiHost: '',
                databaseId: 'db'
            })
        ).toBeInstanceOf(Promise);
    });

    it('should reject promise if loadScript returns error', () => {
        // очистим закешированный вызов getDataSyncApi
        removeCacheApiPromise();

        require('load-script').mockImplementation(
            (_url, callback) => {
                callback(new Error());
            }
        );

        return expect(openDatabase({
            apiHost: '',
            databaseId: 'rejectedPromiseDB'
        })).rejects.toBeTruthy();
    });

    it('processRecord', () => {
        const testObject = {
            foo: 1,
            bar: 2,
            collectionId: 'coll',
            recordId: 'id'
        };

        expect(processRecord({
            getFieldIds: () => Object.keys(testObject),
            getFieldValue: (key) => testObject[key],
            getCollectionId: () => 'collection_id',
            getRecordId: () => 'id'
        })).toEqual(testObject);
    });
});
