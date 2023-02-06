import { formatUrl } from '../formatUrl';

describe('TextFormatter formatUrl', () => {
    describe('#formatUrl', () => {
        it('returns full url', () => {
            expect(formatUrl('ya.ru')).toBe('http://ya.ru');
            expect(formatUrl('http://ya.ru')).toBe('http://ya.ru');
        });

        it('returns empty url', () => {
            expect(formatUrl()).toBe('');
            expect(formatUrl('')).toBe('');
        });
    });
});
