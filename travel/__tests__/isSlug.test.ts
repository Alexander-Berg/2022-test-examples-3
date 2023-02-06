import isSlug from '../isSlug';

describe('isSlug', () => {
    it('Корректные слаги', () => {
        expect(isSlug('test')).toBe(true);
        expect(isSlug('sankt-peterburg')).toBe(true);
        expect(isSlug('test1')).toBe(true);
        expect(isSlug('1test1')).toBe(true);
        expect(isSlug('test1-test2')).toBe(true);
    });

    it('Некорректные слаги', () => {
        expect(isSlug('TEST')).toBe(false);
        expect(isSlug('тест')).toBe(false);
    });
});
