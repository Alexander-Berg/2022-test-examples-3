import React from 'react';
import { ReactWrapper } from 'enzyme';

import { DisplayMskuCategory, DisplayManagerCategory, ManagerRole } from 'src/java/definitions';
import { ManagerCategoriesPage } from 'src/pages/managers/ManagerCategoriesPage';
import {
  resolveCategoriesAll,
  resolveCommonRequests,
  resolveManagerCategoriesAll,
  resolveCategoriesManagerUsers,
} from 'src/test/commonResolve';
import { TestCmApp } from 'src/test/setupApp';
import { expectAllRequestsResolvedExact, sequence, testDataGen } from 'src/test/utils/utils';
import { CategoryRolesFilterForm } from './components/CategoryRolesFilterForm';
import { CategoryRolesTable } from './components/CategoryRolesTable';

jest.useFakeTimers();

const CATEGORY_LEAF_ID_START = 1000;
const CATEGORY_MID_ID_START = 100;
const ONE_MID_CHILDREN_COUNT = 8;

export const testCategoriesTopLevel: DisplayMskuCategory[] = testDataGen({
  id: 0,
  name: 'category-top-',
  parentId: -1,
  published: true,
  cargoTypesOverride: {},
} as any as DisplayMskuCategory).from(...sequence(3));

export const testCategoriesMiddleLevel: DisplayMskuCategory[] = testCategoriesTopLevel.reduce(
  (acc, topLevelCategory) => {
    const children = testDataGen({
      id: CATEGORY_MID_ID_START + topLevelCategory.id,
      name: 'category-middle-',
      published: true,
    } as any as DisplayMskuCategory).from(...sequence(5));
    children.forEach(cat => {
      acc.push({ ...cat, parentId: topLevelCategory.id });
    });
    return acc;
  },
  [] as DisplayMskuCategory[]
);

export const testCategoriesLeaves: DisplayMskuCategory[] = testCategoriesMiddleLevel.reduce((acc, midLevelCategory) => {
  const children = testDataGen({
    id: CATEGORY_LEAF_ID_START + midLevelCategory.id,
    name: 'category-leaf-',
    published: true,
  } as any as DisplayMskuCategory).from(...sequence(ONE_MID_CHILDREN_COUNT));
  children.forEach(cat => {
    acc.push({ ...cat, parentId: midLevelCategory.id });
  });
  return acc;
}, [] as DisplayMskuCategory[]);

const categories = [...testCategoriesTopLevel, ...testCategoriesMiddleLevel, ...testCategoriesLeaves];

testDataGen({
  firstName: 'firstName-',
  lastName: 'lastName-',
  staffLogin: 'staffLogin',
}).from(...sequence(16));

const managers: DisplayManagerCategory[] = testCategoriesLeaves
  .filter(cat => cat.id - CATEGORY_LEAF_ID_START - CATEGORY_MID_ID_START < ONE_MID_CHILDREN_COUNT / 2)
  .map(cat => ({
    categoryId: cat.id,
    categoryName: cat.name,
    categoryUrl: `/category-url-${cat.id}`,
    login: `category-manager-for-${cat.name}-${cat.id}`,
    role: ManagerRole.OTHER,
    userInfo: {
      firstName: `firstName-${cat.id}`,
      lastName: `lastName-${cat.id}`,
      staffLogin: `staffLogin-${cat.id}`,
    },
  }));

describe('ManagerCategoriesPage', () => {
  function getFilter(app: ReactWrapper) {
    return app.find(CategoryRolesFilterForm);
  }

  function getTable(app: ReactWrapper) {
    return app.find(CategoryRolesTable);
  }

  function expectInitialState(app: ReactWrapper<{}, {}, React.Component>) {
    expect(app.find(ManagerCategoriesPage)).toHaveLength(1);
    expect(getFilter(app)).toHaveLength(1);
    expect(getTable(app)).toHaveLength(1);

    // expect(getTable(app).find(Spin)).toHaveLength(1);
  }

  it('basic scenario', () => {
    const { app, api } = new TestCmApp('/manager-categories');
    app.update();
    expect(app.find(ManagerCategoriesPage)).toHaveLength(1);
    resolveCommonRequests(api);

    resolveCategoriesAll(api, categories);
    resolveManagerCategoriesAll(api, managers);
    resolveCategoriesManagerUsers(api);

    expectInitialState(app);

    expectAllRequestsResolvedExact(api);
  });
});
