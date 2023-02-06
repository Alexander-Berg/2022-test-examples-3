import { formatFromNow } from '@src/utils/date';

describe('formatFromNow()', () => {
    it('должен возвращать «в будущем»', () => {
        expect(formatFromNow(
            new Date('2021-11-02').getTime(),
            new Date('2021-11-01 15:00').getTime(),
        )).toBe('в будущем');
    });

    it('должен возвращать «сегодня»', () => {
        expect(formatFromNow(
            new Date('2021-11-01 09:00').getTime(),
            new Date('2021-11-01 15:00').getTime(),
        )).toBe('сегодня');
    });

    it('должен возвращать «вчера»', () => {
        expect(formatFromNow(
            new Date('2021-10-31 15:00').getTime(),
            new Date('2021-11-01 15:00').getTime(),
        )).toBe('вчера');
    });

    it('должен возвращать количество дней', () => {
        expect(formatFromNow(
            new Date('2021-10-26 00:00').getTime(),
            new Date('2021-11-01 00:00').getTime(),
        )).toBe('6 дней назад');
    });

    it('должен возвращать дату за последний год', () => {
        expect(formatFromNow(
            new Date('2021-01-01 00:00').getTime(),
            new Date('2021-11-01 00:00').getTime(),
        )).toBe('1 января');
    });

    it('должен возвращать полную дату при разнице более года', () => {
        expect(formatFromNow(
            new Date('2020-12-01 00:00').getTime(),
            new Date('2021-11-01 00:00').getTime(),
        )).toBe('1 декабря 2020');
    });
});
