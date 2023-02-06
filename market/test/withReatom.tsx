import React, { useEffect } from 'react';
import { Provider, useAction, useAtom } from '@reatom/react';
import { render } from '@testing-library/react';
import { combine, createStore, Atom, Action } from '@reatom/core';

import { setupApi } from './api/setupApi';
import { Api, ApiContext } from 'src/java/Api';
import { Filter } from 'src/filters';
import { filterAtom, setFilterAction } from 'src/pages/ParameterSetting/store/filter.atom';
import { categoryDataAtom, setCategoryData } from 'src/store/categories/categoryData.atom';
import {
  currentCategoryDataAtom,
  currentCategoryIdAtom,
} from 'src/pages/ParameterSetting/store/currentCategoryData.atom';
import { ShopModelView, ShopCategoryStatisticsItem, MarketToShopCategoryItem } from 'src/java/definitions';
import { setAllShopModelsAction, shopModelsAtom } from 'src/store/shopModels/shopModels.atom';
import { UiCategoryData, UiCategoryInfo, UiParamMapping } from 'src/utils/types';
import { setShopIdAction, shopIdAtom } from 'src/store/shop/shopId.atom';
import {
  categoryTreeAtom,
  setCategoryTreeAction,
  categoriesStatMapAtom,
  setCategoryStatisticAction,
} from 'src/store/categories';
import {
  paramMappingsAtom,
  updateParamMappingsAction,
} from 'src/pages/ParameterSetting/store/mappings/paramMappings.atom';
import { shopsAtom, setShopListAction } from 'src/store/shop/shopList.atom';
import { defaultAtoms } from 'src/store/reatom/reatomStore';
import { initApi } from 'src/store/reatom/api.atom';
import { shopCategoriesStatAtom, setAllShopCategoriesAction } from 'src/pages/Categories/store/shopCategoriesStat.atom';
// eslint-disable-next-line import/no-extraneous-dependencies
import { createBrowserHistory } from 'history';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';

const history = createBrowserHistory();

export const initReatomStore = (api: Api, atoms?: Record<string, Atom<any>>, dispatches?: Action<any>[]) => {
  const store = createStore(combine({ ...defaultAtoms, ...atoms }));
  store.dispatch(initApi(api));
  if (dispatches?.length) {
    dispatches.forEach(el => store.dispatch(el));
  }
  // register for introspection
  (window as any).store = () => store.getState();
  return store;
};

export const setupWithReatom = (
  component: React.ReactNode,
  atoms?: Record<string, Atom<any>>,
  dispatches?: Action<any>[]
) => {
  const api = setupApi();
  const reatomStore = initReatomStore(api, atoms, dispatches);
  const app = render(
    <ApiContext.Provider value={api}>
      <QueryParamsProvider history={history}>
        <Provider value={reatomStore}>{component}</Provider>
      </QueryParamsProvider>
    </ApiContext.Provider>
  );

  return { app, api, reatomStore };
};

export const useShopId = (id: number) => {
  const shopId = useAtom(shopIdAtom);
  const setShopId = useAction(setShopIdAction);

  useEffect(() => {
    setShopId(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shopId]);

  return shopId;
};

export const useShopList = (shops: any) => {
  const setShops = useAction(setShopListAction);
  const shopsStore = useAtom(shopsAtom);

  useEffect(() => {
    setShops(shops);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shopsStore]);

  return shopsStore;
};

export const useDescribeCategoryData = (categoryData: UiCategoryData) => {
  useAtom(categoryDataAtom);
  useAtom(currentCategoryIdAtom);
  const currentCategory = useAtom(currentCategoryDataAtom);
  const setCategory = useAction(setCategoryData);
  useEffect(() => {
    setCategory(categoryData);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return currentCategory;
};

export const useDescribeFilterData = (filter: Filter) => {
  useAtom(filterAtom);
  const setFilter = useAction(setFilterAction);
  useEffect(() => {
    setFilter(filter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
};

export const useDescribeAllModels = (models: ShopModelView[]) => {
  const allModels = useAtom(shopModelsAtom);

  const setAllShopModels = useAction(setAllShopModelsAction);

  useEffect(() => {
    setAllShopModels(models);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return allModels;
};

export const useDescribeMapping = (mappings: UiParamMapping[]) => {
  const paramMappings = useAtom(paramMappingsAtom);
  const setMapping = useAction(updateParamMappingsAction);
  useEffect(() => {
    setMapping(mappings);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return paramMappings;
};

export const useDescribeCategoryTree = (tree: Record<number, UiCategoryInfo>) => {
  const categoryTree = useAtom(categoryTreeAtom);
  const setCategories = useAction(setCategoryTreeAction);
  useEffect(() => {
    setCategories(tree);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return categoryTree;
};

export const useDescribeFilter = (filter: Filter) => {
  const setFilter = useAction(setFilterAction);
  useEffect(() => {
    setFilter(filter);
  });
};

export const useDescribeCategoryStat = (stat: ShopCategoryStatisticsItem[]) => {
  const categoryTree = useAtom(categoriesStatMapAtom);
  const setCategories = useAction(setCategoryStatisticAction);
  useEffect(() => {
    setCategories(stat);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return categoryTree;
};

export const useDescribeShopModels = (models: ShopModelView[]) => {
  const allModels = useAtom(shopModelsAtom);
  const setAllShopModels = useAction(setAllShopModelsAction);

  useEffect(() => {
    setAllShopModels(models);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return allModels;
};

export const useDescribeMarketToShopCategories = (categories: MarketToShopCategoryItem[]) => {
  const cat = useAtom(shopCategoriesStatAtom);
  const setCategories = useAction(setAllShopCategoriesAction);

  useEffect(() => {
    setCategories(categories);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return cat;
};
