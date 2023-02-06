import {
    strictEqual,
    deepStrictEqual,
} from 'assert';
import { notifications } from 'notifications';

describe('hasPushId', function() {
    let hasPushId: (pushId?: string) => Promise<boolean>;

    beforeEach(function() {
        hasPushId = require('.').hasPushId;

        delete require.cache[require.resolve('.')];
    });

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

    it('Синхронный запуск', async function() {
        strictEqual(await hasPushId('id1'), false, '1 вызов');
        strictEqual(await hasPushId('id1'), true, '2 вызов');

        const params = await notifications.getParams(['pushIds']);

        deepStrictEqual(params.get('pushIds'), ['id1']);
    });

    it('Параллельный запуск', async function() {
        const map = new Map();

        map.set(
            'pushIds',
            [
                '01', '02', '03', '04', '05', '06', '07', '08', '09', '10',
                '11', '12', '13', '14', '15', '16', '17', '18', '19', '20',
                '21', '22', '23', '24', '25', '26', '27', '28', '29', '30',
                '31', '32', '33', '34', '35', '36', '37', '38', '39', '40',
                '41', '42', '43', '44', '45', '46', '47', '48',
            ],
        );

        await notifications.setParams(map);

        const values = await Promise.all([
            hasPushId('49'),
            hasPushId('49'),
            hasPushId('50'),
            hasPushId('51'),
            hasPushId('52'),
            hasPushId('50'),
        ]);

        deepStrictEqual(values, [false, true, false, false, false, true], 'неверные значения');

        const params = await notifications.getParams(['pushIds']);

        deepStrictEqual(
            params.get('pushIds'),
            [
                '52', '51', '50', '49', '01', '02', '03', '04', '05', '06',
                '07', '08', '09', '10', '11', '12', '13', '14', '15', '16',
                '17', '18', '19', '20', '21', '22', '23', '24', '25', '26',
                '27', '28', '29', '30', '31', '32', '33', '34', '35', '36',
                '37', '38', '39', '40', '41', '42', '43', '44', '45', '46',
            ],
        );
    });
});
