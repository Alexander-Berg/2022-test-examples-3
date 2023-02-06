import { countFormatter } from '../countFormatter';

describe('Count formatter', () => {
    it('test_LessThanThousand', () => {
        expect(countFormatter(1)).toBe('1');
        expect(countFormatter(328)).toBe('328');
        expect(countFormatter(999)).toBe('999');
    });

    it('test_Thousand_To_HundredThousand', () => {
        expect(countFormatter(1000)).toBe('1k');
        expect(countFormatter(1001)).toBe('1k');
        expect(countFormatter(12002)).toBe('12k');
        expect(countFormatter(12099)).toBe('12k');
        expect(countFormatter(12199)).toBe('12.1k');
        expect(countFormatter(12999)).toBe('12.9k');
        expect(countFormatter(99999)).toBe('99.9k');
    });

    it('test_HundredThousand_To_Million', () => {
        expect(countFormatter(100000)).toBe('100k');
        expect(countFormatter(340284)).toBe('340k');
        expect(countFormatter(999999)).toBe('999k');
    });

    it('test_Million_To_HundredMillion', () => {
        expect(countFormatter(1000000)).toBe('1m');
        expect(countFormatter(6239010)).toBe('6.2m');
        expect(countFormatter(62390120)).toBe('62.3m');
        expect(countFormatter(99999999)).toBe('99.9m');
    });

    it('test_HundredMillion_To_Billion', () => {
        expect(countFormatter(100000000)).toBe('100m');
        expect(countFormatter(328000000)).toBe('328m');
        expect(countFormatter(999000000)).toBe('999m');
    });

    it('test_MoreThanBillion', () => {
        expect(countFormatter(1000000000)).toBe('1b');
        expect(countFormatter(1000000000 * 30)).toBe('30b');
        expect(countFormatter(1000000000 * 999)).toBe('999b');
        expect(countFormatter(1000000000 * 1000)).toBe('999b+');
    });
});
