import { assert } from 'chai';
import { YaIDBTransaction } from '.';

const { strictEqual } = assert;

describe('YaIDBTransaction', function() {
    let _database: IDBDatabase;

    beforeEach(function(done: Mocha.Done) {
        const request = indexedDB.open('test');

        request.onupgradeneeded = () => {
            _database = request.result;

            const objectStore = _database.createObjectStore('test');

            objectStore.add(1, 1);
        };
        request.onsuccess = () => done();
        request.onerror = () => done(request.error);
    });

    afterEach(function(done: Mocha.Done) {
        _database.close();

        const request = indexedDB.deleteDatabase('test');

        request.onsuccess = () => done();
        request.onerror = () => done(request.error);
    });

    describe('YaIDBTransaction.createRead()', function() {
        it('Чтение', async function() {
            const transaction = YaIDBTransaction.createRead(_database, 'test');
            const objectStore = transaction.objectStore('test');

            const request1 = objectStore.get(1);
            const request2 = objectStore.get('1');

            await transaction.complete;

            strictEqual(request1.result, 1, '1 запрос');
            strictEqual(request2.result, undefined, '2 запрос');
        });

        it('Изменение', function() {
            const transaction = YaIDBTransaction.createRead(_database, 'test');
            const objectStore = transaction.objectStore('test');

            try {
                objectStore.delete(1);
            } catch (error) {
                strictEqual(error.name, 'ReadOnlyError');
            }
        });
    });

    it('YaIDBTransaction.createWrite()', async function() {
        const transaction = YaIDBTransaction.createWrite(_database, 'test');
        const objectStore = transaction.objectStore('test');

        objectStore.delete(1);
        objectStore.add(2, 2);
        objectStore.put(3, 2);

        const request1 = objectStore.get(1);
        const request2 = objectStore.get(2);

        await transaction.complete;

        strictEqual(request1.result, undefined, '1 запрос');
        strictEqual(request2.result, 3, '2 запрос');
    });

    it('#.complete', function() {
        const transaction = YaIDBTransaction.createRead(_database, 'test');
        const promise = transaction.complete;

        strictEqual(transaction.complete, promise);
    });

    it('#.objectStore()', function() {
        const transaction = YaIDBTransaction.createRead(_database, 'test');
        const objectStore = transaction.objectStore('test');

        strictEqual(objectStore.name, 'test');
    });
});
