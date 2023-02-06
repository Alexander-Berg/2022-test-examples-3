import { assert } from 'chai';
import { YaIDBRequest } from '.';

const { strictEqual } = assert;

describe('YaIDBRequest', function() {
    let _openDBRequest: YaIDBRequest<IDBDatabase>;

    beforeEach(function() {
        _openDBRequest = YaIDBRequest.open('test', 2, database => {
            const objectStore = database.createObjectStore('test');

            objectStore.add('1', '1');
        });
    });

    afterEach(async function() {
        const database = await _openDBRequest.ready;

        database.close();
    });

    afterEach(function(done: Mocha.Done) {
        const request = indexedDB.deleteDatabase('test');

        request.onsuccess = () => done();
        request.onerror = () => done(request.error);
    });

    it('YaIDBRequest.open()', async function() {
        const {
            name,
            version,
            objectStoreNames,
        } = await _openDBRequest.ready;

        strictEqual(name, 'test', 'неверное имя');
        strictEqual(version, 2, 'неверная версия');
        strictEqual(objectStoreNames.contains('test'), true, 'хранилище не найдено');
        strictEqual(objectStoreNames.length, 1, 'общее кол-во хранилищ !== 1');
    });

    it('YaIDBRequest.get()', async function() {
        const database = await _openDBRequest.ready;

        const transaction = database.transaction('test');
        const objectStore = transaction.objectStore('test');

        const request1 = YaIDBRequest.get(objectStore, 1);
        const request2 = YaIDBRequest.get(objectStore, '1');

        strictEqual(await request1.ready, undefined, '1 запрос');
        strictEqual(await request2.ready, '1', '2 запрос');
    });

    it('#.ready', function() {
        const promise = _openDBRequest.ready;

        strictEqual(_openDBRequest.ready, promise);
    });
});
