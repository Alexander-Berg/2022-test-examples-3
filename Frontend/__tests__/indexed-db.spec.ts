import 'fake-indexeddb/auto';
import { DataAccess } from '../indexed-db';
import { IItem } from '../store.types';

interface IData {
    html: string;
    foo: string;
}

const stubItem: IItem<IData> = {
    key: 'test-key',
    data: {
        html: '<div>result</div>',
        foo: 'foo',
    },
};

describe('IndexedDB', function() {
    const meta = {
        liveTime: expect.any(Number),
        versionApp: expect.any(String),
    };

    it('Поддерживает разные названия базы', async function() {
        const db1 = new DataAccess<IData>({
            dbName: 'app-1',
            entities: ['pages'],
            versionApp: 'v0.1',
        });
        const db2 = new DataAccess<IData>({
            dbName: 'app-2',
            entities: ['pages'],
            versionApp: 'v0.1',
        });

        // @ts-ignore private
        jest.spyOn(db1.db, 'disabled', 'get').mockReturnValue(false);
        // @ts-ignore private
        jest.spyOn(db2.db, 'disabled', 'get').mockReturnValue(false);

        await db1.add('pages', stubItem);
        await db2.add('pages', { ...stubItem, key: 'test-key-2' });

        expect(await db1.get('pages', 'test-key')).toEqual({
            ...stubItem,
            meta,
        });
        expect(await db2.get('pages', 'test-key-2')).toEqual({
            ...stubItem,
            key: 'test-key-2',
            meta,
        });
        expect(await db1.get('pages', 'test-key-2')).toBeUndefined();
        expect(await db2.get('pages', 'test-key')).toBeUndefined();
    });

    it('Поддерижвает разные сущности для базы', async function() {
        const db = new DataAccess<IData>({
            dbName: 'app',
            entities: ['pages', 'bundles'],
            versionApp: 'v0.1',
        });

        // @ts-ignore private
        jest.spyOn(db.db, 'disabled', 'get').mockReturnValue(false);

        await db.add('pages', { ...stubItem, key: 'test-key-1' });
        await db.add('bundles', { ...stubItem, key: 'test-key-2' });

        expect(await db.get('pages', 'test-key-1')).toEqual({
            ...stubItem,
            key: 'test-key-1',
            meta,
        });
        expect(await db.get('pages', 'test-key-2')).toBeUndefined();
        expect(await db.get('bundles', 'test-key-1')).toBeUndefined();
        expect(await db.get('bundles', 'test-key-2')).toEqual({
            ...stubItem,
            key: 'test-key-2',
            meta,
        });
    });

    it('Инвалидирует запись в таблице по времени и версии приложения', async function(done) {
        const db = new DataAccess<IData>({
            dbName: 'app',
            entities: ['pages'],
            versionApp: 'v0.1',
            defaultTTL: 300,
        });
        let dbNext = new DataAccess<IData>({
            dbName: 'app-next',
            entities: ['pages'],
            versionApp: 'v0.1',
        });

        // @ts-ignore private
        jest.spyOn(db.db, 'disabled', 'get').mockReturnValue(false);
        // @ts-ignore private
        jest.spyOn(dbNext.db, 'disabled', 'get').mockReturnValue(false);

        await db.add('pages', stubItem);
        await dbNext.add('pages', stubItem);

        expect(await db.get('pages', stubItem.key)).toEqual({
            ...stubItem,
            meta,
        });
        expect(await dbNext.get('pages', stubItem.key)).toEqual({
            ...stubItem,
            meta,
        });

        // Запись протухла по версии
        await dbNext.close();

        dbNext = new DataAccess<IData>({
            dbName: 'app-next',
            entities: ['pages'],
            versionApp: 'v0.2',
        });

        // @ts-ignore private
        jest.spyOn(dbNext.db, 'disabled', 'get').mockReturnValue(false);

        // Запись протухла по версии
        expect(await dbNext.get('pages', stubItem.key)).toBeUndefined();

        // Запись протухла по времени
        setTimeout(async function() {
            expect(await db.get('pages', stubItem.key)).toBeUndefined();

            done();
        }, 500);
    });
});
