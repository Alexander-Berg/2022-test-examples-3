import { assert } from 'chai';
import { YaIDBTransaction } from './lib/YaIDBTransaction';
import { notifications } from '.';

const {
    strictEqual,
    deepStrictEqual,
} = assert;

describe('notifications', function() {
    async function getSizeParams(): Promise<number> {
        const database = await notifications.connection;

        const transaction = YaIDBTransaction.createRead(database, 'params');
        const objectStore = transaction.objectStore('params');
        const request = objectStore.count();

        await transaction.complete;

        return request.result;
    }

    afterEach(async function() {
        const database = await notifications.connection;

        database.close();
        // При программном закрытии соединения, событие close не происходит!
        if (database.onclose) database.onclose(new Event('close'));
    });

    afterEach(function(done: Mocha.Done) {
        const request = indexedDB.deleteDatabase('notifications');

        request.onsuccess = () => done();
        request.onerror = () => done(request.error);
    });

    it('#.connection', async function() {
        const connection = notifications.connection;
        const {
            name,
            version,
            objectStoreNames,
        } = await connection;

        strictEqual(name, 'notifications', 'неверное имя');
        strictEqual(version, 1, 'неверная версия');
        strictEqual(objectStoreNames.contains('params'), true, 'хранилище не найдено');
        strictEqual(objectStoreNames.length, 1, 'общее кол-во хранилищ !== 1');
        strictEqual(notifications.connection, connection, 'неверный тип');
    });

    it('#.getParams()', async function() {
        const params = await notifications.getParams(['userId', 'subscription']);

        deepStrictEqual(
            Array.from(params),
            [
                ['userId', undefined],
                ['subscription', undefined],
            ],
            'неправильная структура',
        );
    });

    it('#.setParams()', async function() {
        const map = new Map();

        map
            .set('userId', 'id1')
            .set('subscription', undefined);

        await notifications.setParams(map);

        const params = await notifications.getParams(map.keys());

        deepStrictEqual(
            Array.from(params),
            [
                ['userId', 'id1'],
                ['subscription', undefined],
            ],
            'неправильная структура',
        );
        strictEqual(await getSizeParams(), 1, 'неправильный размер хранилища');
    });

    it('#.getDifference()', async function() {
        const map1 = new Map();

        map1
            .set('userId', 'id1')
            .set('subscription', 'subscription')
            .set('userAgent', 'ua1');

        await notifications.setParams(map1);

        const map2 = new Map();

        map2
            .set('pushIds', ['1', '2', '3'])
            .set('userAgent', 'ua2')
            .set('subscription', undefined)
            .set('userId', 'id1');

        const params = await notifications.getDifference(map2);

        deepStrictEqual(
            Array.from(params),
            [
                ['pushIds', ['1', '2', '3']],
                ['userAgent', 'ua2'],
                ['subscription', undefined],
            ],
            'неправильная структура',
        );
    });
});
