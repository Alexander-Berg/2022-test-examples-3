import { isExifMarker } from './isExifMarker';

describe('isExifMarker', () => {
    it('должен вернуть true для валидного маркера', () => {
        expect(isExifMarker(0xFF22)).toBe(true);
    });

    it('должен вернуть false для невалидного маркера', () => {
        expect(isExifMarker(0xF322)).toBe(false);
    });
});
