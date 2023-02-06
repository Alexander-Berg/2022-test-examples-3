import { getTZoffset } from './date';

const wrap = (suite: Function) => describe('helpers', () => { describe('date', () => { suite() }) });

wrap(() => describe('getTZoffset', () => {
    it('should parse positive tz', () => {
        const time = '2021-04-30T00:00:00+05:00';

        expect(getTZoffset(time)).toBe(5 * 60 * 60);
    });

    it('should parse negative tz', () => {
        const time = '2021-04-28T00:00:00-04:00';

        expect(getTZoffset(time)).toBe(-4 * 60 * 60);
    });

    it('should return fallback tz', () => {
        const time = '1693298429';

        expect(getTZoffset(time)).toBe(0);
    });
}));
