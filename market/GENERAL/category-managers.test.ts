import { getCategoryManagersStateSelector, getCategoryIdsByManagerSelector } from './category-managers.selectors';
import { CategoryManagersState } from './category-managers.reducer';

describe('store/root/categories selectors', () => {
  const catManagers: CategoryManagersState = {
    categoryIdsByManager: {},
    categoryManagerUsers: {
      isLoaded: false,
      result: [],
    },
    catTeams: {
      isLoaded: false,
      result: [],
    },
  };
  const state = { catManagers };

  it('getCategoryManagersStateSelector', () => {
    expect(getCategoryManagersStateSelector(state)).toBe(catManagers);
  });

  it('getCategoryIdsByManagerSelector', () => {
    expect(getCategoryIdsByManagerSelector(state)).toBe(catManagers.categoryIdsByManager);
  });
});
