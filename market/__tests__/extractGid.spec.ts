import {extractGid} from '../extractGid';

describe('extractGid', () => {
    it('Достаём gid из примитива', () => {
        const number = extractGid(1);
        const stringValue = extractGid('joke');
        const undefinedValue = extractGid();
        const nullValue = extractGid(null);

        expect(nullValue).toEqual(null);
        expect(undefinedValue).toEqual(undefined);
        expect(stringValue).toEqual('joke');
        expect(number).toEqual(1);
    });

    it('Массив объектов в массив gids', () => {
        const arr = extractGid([{gid: '1'}, {gid: '2'}, {gid: '3'}]);

        expect(arr[0]).toEqual('1');
        expect(arr[1]).toEqual('2');
        expect(arr[2]).toEqual('3');
    });

    it('Объект с полем gid', () => {
        const temp = {gid: 'joke'};
        const gid = extractGid(temp);

        expect(gid).toEqual('joke');
    });

    it('Объект без поля gid', () => {
        const temp = {};
        const gid = extractGid(temp);

        expect(gid).toEqual(temp);
    });
});
