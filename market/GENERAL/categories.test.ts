import { getCategoriesStateSelector, getCategoryIdsSelector, getCategoriesByIdSelector } from './categories.selectors';
import { CategoriesState } from './categories.reducer';

describe('store/root/categories', () => {
  describe('selectors', () => {
    it('getCategoriesStateSelector', () => {
      const categories: CategoriesState = { ids: [], byId: {} };
      const state: { categories: CategoriesState } = { categories };

      expect(getCategoriesStateSelector(state)).toBe(categories);
    });

    it('getCategoryIdsSelector', () => {
      const categories: CategoriesState = { ids: [], byId: {} };
      const state: { categories: CategoriesState } = { categories };

      expect(getCategoryIdsSelector(state)).toBe(categories.ids);
    });

    it('getCategoriesByIdSelector', () => {
      const categories: CategoriesState = { ids: [], byId: {} };
      const state: { categories: CategoriesState } = { categories };

      expect(getCategoriesByIdSelector(state)).toBe(categories.byId);
    });
  });
});
