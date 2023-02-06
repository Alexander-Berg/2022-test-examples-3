import { formatDate } from './index';

describe('formatDate', () => {
    let date;

    it('2019-01-22T09:55:06.697132', () => {
        date = new Date('2019-01-22T09:55:06.697132');
        expect(formatDate(date)).toEqual('2019-01-22 09:55:06');
    });

    it('1932-06-01T01:59:59.697132', () => {
        date = new Date('1932-06-01T01:59:59.697132');
        expect(formatDate(date)).toEqual('1932-06-01 01:59:59');
    });

    it('2019-01-01T00:00:00.0', () => {
        date = new Date('2019-01-01T00:00:00.0');
        expect(formatDate(date)).toEqual('2019-01-01 00:00:00');
    });

    it('1900-12-31T23:59:59.2434320', () => {
        date = new Date('1900-12-31T23:59:59.2434320');
        expect(formatDate(date)).toEqual('1900-12-31 23:59:59');
    });
});
