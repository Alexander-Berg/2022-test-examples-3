import { getSearchQueryWithText, parseSearchQuery } from './search';

describe('Функции для работы с GET параметром text', () => {
    it('parseSearchQuery с заданным GET параметром text', () => {
        expect(parseSearchQuery('?text=TEST')).toBe('TEST');
    });

    it('parseSearchQuery без GET параметра text', () => {
        expect(parseSearchQuery('')).toBe('');
    });

    it('getSearchQueryWithText c заданным GET параметром text', () => {
        expect(getSearchQueryWithText('?text=123123&second=123', 'TEST')).toBe('?text=TEST&second=123');
    });

    it('getSearchQueryWithText без GET параметра text', () => {
        expect(getSearchQueryWithText('?second=123', 'TEST')).toBe('?second=123&text=TEST');
    });
});
