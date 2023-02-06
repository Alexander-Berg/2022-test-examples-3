import { isTogglingFavorite, isInFavorite } from '../../../components/redux/components/favorite/helpers';

describe('favorites selectors', () => {
    describe('isTogglingFavorite', () => {
        it('should return false if favorite is not set', () => {
            expect(Boolean(isTogglingFavorite({}))).toBe(false);
        });

        it('should return true if adding to favorite', () => {
            expect(isTogglingFavorite({ favorite: { state: true } })).toBe(true);
        });

        it('should return true if removing from favorite', () => {
            expect(isTogglingFavorite({ favorite: { state: false, itemId: 'itemId' } })).toBe(true);
        });

        it('should return false in other cases', () => {
            expect(isTogglingFavorite({ favorite: { state: false } })).toBe(false);
            expect(isTogglingFavorite({ favorite: { state: true, itemId: 'itemId' } })).toBe(false);
            expect(isTogglingFavorite({ favorite: {} })).toBe(false);
        });
    });

    describe('isInFavorite', () => {
        it('should return false if favorite is not set', () => {
            expect(Boolean(isInFavorite({}))).toBe(false);
        });

        it('should return true if resource is in favorite', () => {
            expect(isInFavorite({ favorite: { state: true } })).toBe(true);
        });

        it('should return false if resource is not in favorite', () => {
            expect(isInFavorite({ favorite: { state: false } })).toBe(false);
        });
    });
});
