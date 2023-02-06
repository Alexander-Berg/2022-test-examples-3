import { MovementDirection, PaidAction, RequestedGoodMbocState, RequestedMovementState } from 'src/rest/definitions';
import configureStore from 'src/store/configureStore';
import { EpicDependencies } from 'src/store/types';
import { createBrowserHistory } from 'history';
import { getType } from 'typesafe-actions';
import RestService from '../../services/RestService';

import * as actions from './actions';
import {
  getActiveTabId,
  getBillingStatsFilter,
  getCategoryConfig,
  getCategoryTree,
  getCurrentCategoryConfig,
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
  getReferenceCategoryConfig,
  getSelectedCategory,
  getSelectedCategoryId,
  getSelectedGoodType,
  getSelectedMovementId,
  getSelfTariffs,
  getStats,
  getTariffs,
  getWarehouses,
} from './getters';
import { ActiveTabId } from './types';
import { selectAdminPageState } from './selectors';

const history = createBrowserHistory();
const dependencies: EpicDependencies = {
  api: new RestService(),
  history,
};
const store = configureStore({ dependencies });

const getAdminPageState = () => selectAdminPageState(store.getState());
const dispatch = (actionType: any, payload?: any) => store.dispatch({ type: getType(actionType), payload });

const testMovements = {
  items: [
    {
      goodCount: 1,
      movement: {
        createdDate: '',
        direction: MovementDirection.INCOMING,
        id: 1,
        modifiedDate: '',
        state: RequestedMovementState.NEW,
      },
      warehouseFrom: '1',
      warehouseTo: '2',
    },
    {
      goodCount: 1,
      movement: {
        createdDate: '',
        direction: MovementDirection.INCOMING,
        id: 2,
        modifiedDate: '',
        state: RequestedMovementState.NEW,
      },
      warehouseFrom: '1',
      warehouseTo: '2',
    },
  ],
  totalCount: 2,
};
const testMovementWithGoods = {
  goods: [],
  movementWithStats: {
    goodCount: 1,
    movement: {
      createdDate: '',
      direction: MovementDirection.INCOMING,
      id: 1,
      modifiedDate: '',
      state: RequestedMovementState.NEW,
    },
    warehouseFrom: '1',
    warehouseTo: '2',
  },
};

describe('AdminPage reducer', () => {
  it('category tree actions', () => {
    const testTree = [
      {
        hid: 1,
        name: '1',
      },
    ];
    expect(getCategoryTree(getAdminPageState())).toBeUndefined();
    dispatch(actions.getCategoryTree.success, testTree);
    expect(getCategoryTree(getAdminPageState())).toEqual(testTree);
    dispatch(actions.getCategoryTree.failure);
    expect(getCategoryTree(getAdminPageState())).toEqual([]);
  });

  it('category config actions', () => {
    const testConfig = {
      hasEditorInstruction: false,
      hasPhotoEditInstruction: false,
      hasPhotoInstruction: false,
      id: 1,
      modifiedDate: '',
      name: '',
    };
    const testEffectiveConfig = {
      ...testConfig,
      hasEditorInstruction: true,
    };
    const testConfigWithEffective = {
      config: { ...testConfig },
      effectiveConfig: { ...testEffectiveConfig },
    };
    expect(getCategoryConfig(getAdminPageState())).toBeUndefined();
    expect(getCurrentCategoryConfig(getAdminPageState())).toBeUndefined();
    expect(getEffectiveCategoryConfig(getAdminPageState())).toBeUndefined();
    expect(getReferenceCategoryConfig(getAdminPageState())).toBeUndefined();
    dispatch(actions.getCategoryConfig.success, testConfigWithEffective);
    expect(getCategoryConfig(getAdminPageState())).toEqual(testConfigWithEffective);
    expect(getCurrentCategoryConfig(getAdminPageState())).toEqual(testConfig);
    expect(getEffectiveCategoryConfig(getAdminPageState())).toEqual(testEffectiveConfig);
    expect(getReferenceCategoryConfig(getAdminPageState())).toEqual(testConfig);
    dispatch(actions.getCategoryConfig.failure);
    expect(getCategoryConfig(getAdminPageState())).toBeUndefined();
    expect(getCurrentCategoryConfig(getAdminPageState())).toBeUndefined();
    expect(getEffectiveCategoryConfig(getAdminPageState())).toBeUndefined();
    expect(getReferenceCategoryConfig(getAdminPageState())).toBeUndefined();
    dispatch(actions.saveCategoryConfig.success, testConfigWithEffective);
    expect(getCategoryConfig(getAdminPageState())).toEqual(testConfigWithEffective);
    expect(getCurrentCategoryConfig(getAdminPageState())).toEqual(testConfig);
    expect(getEffectiveCategoryConfig(getAdminPageState())).toEqual(testEffectiveConfig);
    expect(getReferenceCategoryConfig(getAdminPageState())).toEqual(testConfig);
    dispatch(actions.setCurrentCategoryConfig, {
      ...testConfig,
      goodTypeId: 1,
    });
    expect(getCurrentCategoryConfig(getAdminPageState())).toEqual({
      ...testConfig,
      goodTypeId: 1,
    });
  });

  it('good types actions', () => {
    const testGoodTypes = [
      {
        displayName: 'test1',
        id: 1,
        modifiedDate: '',
      },
      {
        displayName: 'test2',
        id: 2,
        modifiedDate: '',
      },
      {
        id: 3,
        modifiedDate: '',
      },
    ];
    const expectedOptions = [
      {
        val: 1,
        text: '1:test1',
      },
      {
        val: 2,
        text: '2:test2',
      },
      {
        val: 3,
        text: '3',
      },
      {
        val: 0,
        text: 'Не указан',
      },
    ];
    expect(getGoodTypes(getAdminPageState())).toBeUndefined();
    expect(getGoodTypesOptions(getAdminPageState())).toBeUndefined();
    dispatch(actions.getGoodTypes.success, testGoodTypes);
    expect(getGoodTypes(getAdminPageState())).toEqual(testGoodTypes);
    expect(getGoodTypesOptions(getAdminPageState())).toEqual(expectedOptions);
    dispatch(actions.getGoodTypes.failure);
    expect(getGoodTypes(getAdminPageState())).toBeUndefined();
    expect(getGoodTypesOptions(getAdminPageState())).toBeUndefined();
  });

  it('good type actions', () => {
    const testGoodType = {
      displayName: 'test1',
      id: 1,
      modifiedDate: '',
    };
    expect(getSelectedGoodType(getAdminPageState())).toBeUndefined();
    dispatch(actions.getGoodType.success, testGoodType);
    expect(getSelectedGoodType(getAdminPageState())).toEqual(testGoodType);
    dispatch(actions.deleteGoodType.success);
    expect(getSelectedGoodType(getAdminPageState())).toBeUndefined();
    dispatch(actions.saveGoodType.success, testGoodType);
    expect(getSelectedGoodType(getAdminPageState())).toEqual(testGoodType);
  });

  it('billing stats actions', () => {
    expect(getBillingStatsFilter(getAdminPageState())).toBeUndefined();
    expect(getStats(getAdminPageState())).toBeUndefined();
    dispatch(actions.getStats.request, {});
    expect(getBillingStatsFilter(getAdminPageState())).toEqual({});
    dispatch(actions.getStats.success, []);
    expect(getStats(getAdminPageState())).toEqual([]);
  });

  it('select category action', () => {
    const testCategory = {
      hid: 1,
      name: 'test1',
    };
    expect(getSelectedCategory(getAdminPageState())).toBeUndefined();
    dispatch(actions.setSelectedCategory, testCategory);
    expect(getSelectedCategory(getAdminPageState())).toEqual(testCategory);
    expect(getSelectedCategoryId(getAdminPageState())).toEqual(1);
  });

  it('tariffs actions', () => {
    const testSelfTariffs = [
      {
        categoryId: 1,
        paidAction: PaidAction.GOOD_ACCEPT,
        paidActionName: 'test1',
        priceKopeck: 1,
      },
      {
        categoryId: 1,
        paidAction: PaidAction.GOOD_ADD_TO_CART,
        paidActionName: 'test2',
        priceKopeck: 2,
      },
    ];
    const testInheritedTariffs = [
      {
        categoryId: 2,
        paidAction: PaidAction.GOOD_ADD_TO_OUTGOING,
        paidActionName: 'test3',
        priceKopeck: 3,
      },
    ];
    const testTariffs = [...testSelfTariffs, ...testInheritedTariffs];
    expect(getTariffs(getAdminPageState())).toBeUndefined();
    expect(getInheritedTariffs(getAdminPageState())).toBeUndefined();
    expect(getSelfTariffs(getAdminPageState())).toBeUndefined();
    dispatch(actions.setSelectedCategory, {
      hid: 1,
      name: 'test1',
    });
    dispatch(actions.getTariffs.success, testTariffs);
    expect(getTariffs(getAdminPageState())).toEqual(testTariffs);
    expect(getInheritedTariffs(getAdminPageState())).toEqual(testInheritedTariffs);
    expect(getSelfTariffs(getAdminPageState())).toEqual(testSelfTariffs);
    dispatch(actions.getTariffs.failure);
    expect(getTariffs(getAdminPageState())).toBeUndefined();
    expect(getInheritedTariffs(getAdminPageState())).toBeUndefined();
    expect(getSelfTariffs(getAdminPageState())).toBeUndefined();
  });

  it('set selected movement id', () => {
    expect(getSelectedMovementId(getAdminPageState())).toBeUndefined();
    dispatch(actions.setSelectedMovement, 1);
    expect(getSelectedMovementId(getAdminPageState())).toEqual(1);
  });

  it('movements filter and paging', () => {
    const testFilter = { ids: [1] };
    const testPaging1 = {
      page: 1,
      pageSize: 100,
    };
    const testPaging2 = {
      page: 2,
      pageSize: 100,
    };
    expect(getMovementsFilter(getAdminPageState())).toEqual({});
    expect(getMovementsPaging(getAdminPageState())).toEqual(testPaging1);
    dispatch(actions.getMovements.request, { filter: testFilter });
    expect(getMovementsFilter(getAdminPageState())).toEqual(testFilter);
    expect(getMovementsPaging(getAdminPageState())).toEqual(testPaging1);
    dispatch(actions.getMovements.request, { paging: testPaging2 });
    expect(getMovementsFilter(getAdminPageState())).toEqual(testFilter);
    expect(getMovementsPaging(getAdminPageState())).toEqual(testPaging2);
    dispatch(actions.getMovements.request, {});
    expect(getMovementsFilter(getAdminPageState())).toEqual(testFilter);
    expect(getMovementsPaging(getAdminPageState())).toEqual(testPaging2);
    dispatch(actions.setMovementsFilter, {});
    expect(getMovementsFilter(getAdminPageState())).toEqual({});
    expect(getMovementsPaging(getAdminPageState())).toEqual(testPaging2);
  });

  it('get movements actions', () => {
    expect(getMovements(getAdminPageState())).toEqual({
      items: [],
      totalCount: 0,
    });
    dispatch(actions.getMovements.success, testMovements);
    expect(getMovements(getAdminPageState())).toEqual(testMovements);
  });

  it('goods filter and paging', () => {
    const testFilter = { ids: [1] };
    const testPaging1 = {
      page: 1,
      pageSize: 100,
    };
    const testPaging2 = {
      page: 2,
      pageSize: 100,
    };
    expect(getGoodsFilter(getAdminPageState())).toEqual({});
    expect(getGoodsPaging(getAdminPageState())).toEqual(testPaging1);

    dispatch(actions.getGoods.request, { filter: testFilter });
    expect(getGoodsFilter(getAdminPageState())).toEqual(testFilter);
    expect(getGoodsPaging(getAdminPageState())).toEqual(testPaging1);
    dispatch(actions.getGoods.request, { paging: testPaging2 });
    expect(getGoodsFilter(getAdminPageState())).toEqual(testFilter);
    expect(getGoodsPaging(getAdminPageState())).toEqual(testPaging2);
    dispatch(actions.setGoodsFilter, {});
    expect(getGoodsFilter(getAdminPageState())).toEqual({});
    expect(getGoodsPaging(getAdminPageState())).toEqual(testPaging2);
  });

  it('goods actions', () => {
    const testGoods = {
      items: [
        {
          categoryName: 'test1',
          good: {
            createdDate: '',
            id: 1,
            lastPushedMbocState: RequestedGoodMbocState.CL_FAILED,
            mbocState: RequestedGoodMbocState.CL_FAILED,
            modifiedDate: '',
            supplierId: 1,
            supplierSkuId: 'test1',
          },
          stocks: [],
        },
      ],
      totalCount: 0,
    };
    expect(getGoods(getAdminPageState())).toEqual({
      items: [],
      totalCount: 0,
    });
    dispatch(actions.getGoods.success, testGoods);
    expect(getGoods(getAdminPageState())).toEqual(testGoods);
  });

  it('warehouses actions', () => {
    expect(getWarehouses(getAdminPageState())).toBeUndefined();
    dispatch(actions.getWarehouses.success, []);
    expect(getWarehouses(getAdminPageState())).toEqual([]);
  });

  it('movement with goods actions', () => {
    expect(getMovementWithGoods(getAdminPageState())).toBeUndefined();
    expect(getActiveTabId(getAdminPageState())).toEqual(ActiveTabId.CATEGORIES);
    dispatch(actions.getMovementWithGoods.success, testMovementWithGoods);
    expect(getMovementWithGoods(getAdminPageState())).toEqual(testMovementWithGoods);
    expect(getActiveTabId(getAdminPageState())).toEqual(ActiveTabId.MOVEMENT);
  });

  it('movement plan actions', () => {
    const testMovement1 = {
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
    };
    dispatch(actions.getMovements.success, testMovements);
    dispatch(actions.getMovementWithGoods.success, testMovementWithGoods);
    dispatch(actions.planMovement.success, testMovement1);
    expect(getMovements(getAdminPageState()).items![0].movement.state).toEqual(RequestedMovementState.PLANNED);
    expect(getMovementWithGoods(getAdminPageState())!.movementWithStats.movement.state).toEqual(
      RequestedMovementState.PLANNED
    );
    testMovement1.movement.state = RequestedMovementState.NEW;
    dispatch(actions.undoPlanMovement.success, testMovement1);
    expect(getMovements(getAdminPageState()).items![0].movement.state).toEqual(RequestedMovementState.NEW);
    expect(getMovementWithGoods(getAdminPageState())!.movementWithStats.movement.state).toEqual(
      RequestedMovementState.NEW
    );
    testMovement1.movement.state = RequestedMovementState.PLANNED;
    testMovement1.movement.id = 2;
    dispatch(actions.planMovement.success, testMovement1);
    expect(getMovements(getAdminPageState()).items![0].movement.state).toEqual(RequestedMovementState.NEW);
    expect(getMovements(getAdminPageState()).items![1].movement.state).toEqual(RequestedMovementState.PLANNED);
    expect(getMovementWithGoods(getAdminPageState())!.movementWithStats.movement.state).toEqual(
      RequestedMovementState.NEW
    );
  });

  it('set active tab id', () => {
    dispatch(actions.setActiveTab, ActiveTabId.MOVEMENTS);
    expect(getActiveTabId(getAdminPageState())).toEqual(ActiveTabId.MOVEMENTS);
    dispatch(actions.getMovementWithGoods.success, testMovementWithGoods);
    expect(getActiveTabId(getAdminPageState())).toEqual(ActiveTabId.MOVEMENT);
    dispatch(actions.setActiveTab, ActiveTabId.MOVEMENT);
    expect(getMovementWithGoods(getAdminPageState())).toEqual(testMovementWithGoods);
    dispatch(actions.setActiveTab, ActiveTabId.CATEGORIES);
    expect(getMovementWithGoods(getAdminPageState())).toBeUndefined();
  });
});
