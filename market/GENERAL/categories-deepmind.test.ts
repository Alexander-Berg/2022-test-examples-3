import {
  getCategoriesStateSelector,
  getCategoryIdsSelector,
  getCategoriesByIdSelector,
  makeGetCategorySettingsByIdSelector,
} from 'src/store/root/categories-deepmind/categories-deepmind.selectors';
import { CategoriesState } from 'src/store/root/categories-deepmind/categories-deepmind.reducer';
import { CategorySettingsState } from 'src/store/root/category-settings/category-settings.reducer';
import { Category } from '../types';
import { DisplayCategorySettings } from 'src/java/definitions';

const defaultCategory: Category = {
  id: 1,
  parentId: 0,
  fullName: 'test',
  cargoTypesOverride: {},
  name: 'testName',
  path: [],
  children: [],
  published: true,
};

const makeCategory = (params: Partial<Category>): Category => {
  return { ...defaultCategory, ...params };
};

const defaultCategorySettings: DisplayCategorySettings = {
  categoryId: 0,
  modifiedAt: '',
  seasonId: 1,
};

const makeCategorySettings = (params: Partial<DisplayCategorySettings>): DisplayCategorySettings => {
  return { ...defaultCategorySettings, ...params };
};

describe('store/root/categories', () => {
  describe('selectors', () => {
    it('getCategoriesStateSelector', () => {
      const catDeepmind: CategoriesState = { ids: [], byId: {} };
      const state: { catDeepmind: CategoriesState } = { catDeepmind };

      expect(getCategoriesStateSelector(state)).toBe(catDeepmind);
    });

    it('getCategoryIdsSelector', () => {
      const catDeepmind: CategoriesState = { ids: [], byId: {} };
      const state: { catDeepmind: CategoriesState } = { catDeepmind };

      expect(getCategoryIdsSelector(state)).toBe(catDeepmind.ids);
    });

    it('getCategoriesByIdSelector', () => {
      const catDeepmind: CategoriesState = { ids: [], byId: {} };
      const state: { catDeepmind: CategoriesState } = { catDeepmind };

      expect(getCategoriesByIdSelector(state)).toBe(catDeepmind.byId);
    });

    it('makeGetCategorySettingsByIdSelector if dont have own settings get parent settings', () => {
      const category1 = makeCategory({ id: 1, parentId: 2 });
      const category2 = makeCategory({ id: 2, parentId: 0, children: [1] });
      const category2Settings = makeCategorySettings({ categoryId: 2, seasonId: 2 });
      const catDeepmind: CategoriesState = {
        ids: [1, 2, 3],
        byId: {
          1: category1,
          2: category2,
        },
      };
      const categorySettings: CategorySettingsState = {
        byId: {
          2: category2Settings,
        },
      };
      const state = {
        catDeepmind,
        categorySettings,
      };
      const selector1 = makeGetCategorySettingsByIdSelector(1);
      expect(selector1(state)).toStrictEqual({ ...category2Settings, isInheritance: true });
    });

    it('makeGetCategorySettingsByIdSelector if have own settings get it', () => {
      const category1 = makeCategory({ id: 1, parentId: 0 });
      const category1Settings = makeCategorySettings({ categoryId: 1, seasonId: 1 });
      const catDeepmind: CategoriesState = {
        ids: [1],
        byId: {
          1: category1,
        },
      };
      const categorySettings: CategorySettingsState = {
        byId: {
          1: category1Settings,
        },
      };
      const state = {
        catDeepmind,
        categorySettings,
      };
      const selector = makeGetCategorySettingsByIdSelector(1);
      expect(selector(state)).toStrictEqual({ ...category1Settings, isInheritance: false });
    });

    it('makeGetCategorySettingsByIdSelector return undefined if no settings provided', () => {
      const category1 = makeCategory({ id: 1, parentId: 0 });
      const catDeepmind: CategoriesState = {
        ids: [1],
        byId: {
          1: category1,
        },
      };
      const categorySettings: CategorySettingsState = {
        byId: {},
      };
      const state = {
        catDeepmind,
        categorySettings,
      };
      const selector = makeGetCategorySettingsByIdSelector(1);
      expect(selector(state)).toBeUndefined();
    });
  });
});
