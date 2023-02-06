import {
  getActiveTabId,
  getBillingStatsFilter,
  getCategoryConfig,
  getCategoryTree,
  getEffectiveCategoryConfig,
  getGoods,
  getGoodsFilter,
  getGoodsPaging,
  getGoodTypes,
  getGoodTypesOptions,
  getInheritedTariffs,
  getMovements,
  getMovementsFilter,
  getMovementsPaging,
  getMovementWithGoods,
  getSelectedCategoryId,
  getSelectedGoodType,
  getSelectedMovementId,
  getSelfTariffs,
  getStats,
  getTariffs,
  getWarehouses,
  isCategoryConfigModified,
} from 'src/pages/Admin/getters';
import { ActiveTabId, AdminPageState } from 'src/pages/Admin/types';
import { MovementDirection, RequestedMovementState } from 'src/rest/definitions';

let adminPageState: AdminPageState;
const defaultCategoryConfig = {
  hasEditorInstruction: false,
  hasPhotoEditInstruction: false,
  hasPhotoInstruction: false,
  id: 123,
  modifiedDate: '123',
  name: '123',
};

beforeAll(() => {
  adminPageState = {
    activeTabId: ActiveTabId.CATEGORIES,
    categoryTree: [],
    goodTypes: [],
    goodTypesOptions: [],
    selectedCategory: { hid: 123, name: '' },
    stats: [],
    tariffs: [],
    inheritedTariffs: [],
    selfTariffs: [],
    billingStatsFilter: {},
    movementsFilter: {},
    movementsPaging: {},
    movements: { items: [], totalCount: 0 },
    goodsFilter: {},
    goodsPaging: {},
    goods: { items: [], totalCount: 0 },
    userIds: [],
  };
});

describe('admin page getters', () => {
  it('simple getters', () => {
    expect(getSelectedCategoryId(adminPageState)).toEqual(123);
    expect(getCategoryTree(adminPageState)).toEqual([]);
    expect(getCategoryConfig(adminPageState)).toBeUndefined();
    adminPageState.categoryConfig = {
      config: {
        hasEditorInstruction: false,
        hasPhotoEditInstruction: false,
        hasPhotoInstruction: false,
        id: 1,
        modifiedDate: '',
        name: '',
      },
      effectiveConfig: {
        hasEditorInstruction: false,
        hasPhotoEditInstruction: false,
        hasPhotoInstruction: false,
        id: 1,
        modifiedDate: '',
        name: '',
      },
    };
    expect(getCategoryConfig(adminPageState)).toBeDefined();
    expect(getEffectiveCategoryConfig(adminPageState)).toBeUndefined();
    adminPageState.effectiveCategoryConfig = {
      hasEditorInstruction: false,
      hasPhotoEditInstruction: false,
      hasPhotoInstruction: false,
      id: 1,
      modifiedDate: '',
      name: '',
    };
    expect(getEffectiveCategoryConfig(adminPageState)).toBeDefined();
    expect(getGoodTypes(adminPageState)).toEqual([]);
    expect(getGoodTypesOptions(adminPageState)).toEqual([]);
    expect(getSelectedGoodType(adminPageState)).toBeUndefined();
    adminPageState.selectedGoodType = {
      id: 1,
      modifiedDate: '',
    };
    expect(getSelectedGoodType(adminPageState)).toBeDefined();
    expect(getStats(adminPageState)).toEqual([]);
    expect(getBillingStatsFilter(adminPageState)).toEqual({});
    expect(getTariffs(adminPageState)).toEqual([]);
    expect(getSelfTariffs(adminPageState)).toEqual([]);
    expect(getInheritedTariffs(adminPageState)).toEqual([]);
    expect(getMovements(adminPageState)).toEqual({ items: [], totalCount: 0 });
    expect(getMovementsFilter(adminPageState)).toEqual({});
    expect(getMovementsPaging(adminPageState)).toEqual({});
    expect(getMovementWithGoods(adminPageState)).toBeUndefined();
    adminPageState.movementWithGoods = {
      goods: [],
      movementWithStats: {
        goodCount: 1,
        movement: {
          createdDate: '',
          direction: MovementDirection.INCOMING,
          id: 1,
          modifiedDate: '',
          state: RequestedMovementState.PLANNED,
        },
        warehouseFrom: '1',
        warehouseTo: '2',
      },
    };
    expect(getMovementWithGoods(adminPageState)).toBeDefined();
    expect(getSelectedMovementId(adminPageState)).toBeUndefined();
    adminPageState.selectedMovementId = 1;
    expect(getSelectedMovementId(adminPageState)).toEqual(1);
    expect(getWarehouses(adminPageState)).toBeUndefined();
    adminPageState.warehouses = [];
    expect(getWarehouses(adminPageState)).toEqual([]);
    expect(getGoodsFilter(adminPageState)).toEqual({});
    expect(getGoodsPaging(adminPageState)).toEqual({});
    expect(getGoods(adminPageState)).toEqual({ items: [], totalCount: 0 });
    expect(getActiveTabId(adminPageState)).toEqual(ActiveTabId.CATEGORIES);
  });

  it('is category config modified', () => {
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.currentCategoryConfig = defaultCategoryConfig;
    adminPageState.referenceCategoryConfig = defaultCategoryConfig;
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.currentCategoryConfig = { ...defaultCategoryConfig };
    adminPageState.referenceCategoryConfig = { ...defaultCategoryConfig };
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.currentCategoryConfig!.minRawPhotos = 2;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.currentCategoryConfig = { ...defaultCategoryConfig };
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.currentCategoryConfig!.minProcessedPhotos = 2;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.currentCategoryConfig = { ...defaultCategoryConfig };
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.currentCategoryConfig!.goodTypeId = 2;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.currentCategoryConfig = undefined;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.currentCategoryConfig = { ...defaultCategoryConfig };
    adminPageState.referenceCategoryConfig!.minRawPhotos = 2;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.referenceCategoryConfig = { ...defaultCategoryConfig };
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.referenceCategoryConfig!.minProcessedPhotos = 2;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.referenceCategoryConfig = { ...defaultCategoryConfig };
    expect(isCategoryConfigModified(adminPageState)).toEqual(false);
    adminPageState.referenceCategoryConfig!.goodTypeId = 2;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
    adminPageState.referenceCategoryConfig = undefined;
    expect(isCategoryConfigModified(adminPageState)).toEqual(true);
  });
});
