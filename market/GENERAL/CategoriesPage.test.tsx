import { act, fireEvent, RenderResult, waitFor } from '@testing-library/react';
import React, { FC } from 'react';
import userEvent from '@testing-library/user-event';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { shopModel, categoryData } from 'src/test/data';
import { TestingRouter } from 'src/test/setupApp';
import { setupWithReatom, useDescribeCategoryTree } from 'src/test/withReatom';
import CategoriesPage from './CategoriesPage';
import { resolveLoadModelsRequest, resolveSaveModelsRequest } from 'src/test/api/resolves';
import { Api } from 'src/java/Api';
import { MarketToShopCategoryItem } from 'src/java/definitions';

const CHANGEABLE_MODELS_URL = '/categories?shopId=10451772&canChangeCategory=1';
const UNCHANGEABLE_MODELS_URL = '/categories?shopId=10451772&canChangeCategory=0';

const goodCategoryGroup = {
  categoryIds: [5],
  id: 1,
  name: 'Text category',
};

const TestApp: FC<{ route: string }> = ({ route }) => {
  useDescribeCategoryTree({ [categoryData.hid]: categoryData });
  return (
    // eslint-disable-next-line react/jsx-curly-brace-presence
    <TestingRouter route={route}>
      <CategoriesPage />
    </TestingRouter>
  );
};

const toggleMarketCategory = (app: RenderResult) => {
  const marketCategoryBlock = app.getByText(categoryData.name);
  fireEvent.click(marketCategoryBlock);
};

const toggleShopCategory = (app: RenderResult) => {
  const shopCategoryBlock = app.getByText(shopModel.shopCategoryName);
  fireEvent.click(shopCategoryBlock);
};

const checkModel = async (app: RenderResult) => {
  // ждем появление товара в таблице
  await app.findByText(shopModel.shopSku);

  const checkbox = app.container.querySelector('.react-grid-checkbox-container');
  expect(checkbox).toBeTruthy();
  fireEvent.click(checkbox!);
};

const confirmCategoryModel = { ...shopModel, marketCategoryConfidence: 9.9, marketCategoryChecked: true };

const resolveStatistic = (api: MockedApiObject<Api>, stat: MarketToShopCategoryItem[]) => {
  act(() => {
    api.shopModelController.getMarketToShopCategoryStatistics.next().resolve(stat);
  });
};
describe('CategoriesPage', () => {
  const callYm = jest.fn();
  window.ym = callYm;

  test('accept category', async () => {
    const { app, api } = setupWithReatom(<TestApp route={CHANGEABLE_MODELS_URL} />);

    resolveStatistic(api, [
      { shopCategoryName: shopModel.shopCategoryName, marketCategoryId: categoryData.hid, shopModelTotal: 1 },
    ]);

    // разворачиваем категорию магазина
    toggleShopCategory(app);
    // // разворачиваем категорию маркета
    toggleMarketCategory(app);

    resolveLoadModelsRequest(api, [shopModel]);

    // выбираем товар для подтверждения категории
    await checkModel(app);

    const acceptCategory = app.getByText('Подтвердить категорию');
    userEvent.click(acceptCategory);

    await waitFor(() => expect(api.shopModelController.updateModelsV2.activeRequests()).toHaveLength(1));

    resolveSaveModelsRequest(api, [confirmCategoryModel]);
    // проверяем что после сохранения товара уходят запросы на обновление статистики
    await waitFor(() => expect(api.goodsGroupController.activeRequests()).toHaveLength(1));
    await waitFor(() => expect(api.shopModelController.activeRequests()).toHaveLength(1));

    // после подтверждения категории таблица с товарами не должна сворачиваться
    app.getByText(shopModel.shopSku);
  });

  test('accept category (enter + ctrl)', async () => {
    const { app, api } = setupWithReatom(<TestApp route={CHANGEABLE_MODELS_URL} />);

    resolveStatistic(api, [
      { shopCategoryName: shopModel.shopCategoryName, marketCategoryId: categoryData.hid, shopModelTotal: 1 },
    ]);

    toggleShopCategory(app);
    toggleMarketCategory(app);

    resolveLoadModelsRequest(api, [shopModel]);

    await checkModel(app);

    fireEvent.keyDown(app.container, { key: 'Enter', ctrlKey: true });
    await waitFor(() => expect(api.shopModelController.activeRequests()).toHaveLength(1));

    resolveSaveModelsRequest(api, [confirmCategoryModel]);
    // проверяем что после сохранения товара уходят запросы на обновление статистики
    await waitFor(() => expect(api.goodsGroupController.activeRequests()).toHaveLength(1));
    await waitFor(() => expect(api.shopModelController.activeRequests()).toHaveLength(1));
  });

  test('when select no changed category', async () => {
    const { app, api } = setupWithReatom(<TestApp route={UNCHANGEABLE_MODELS_URL} />);

    resolveStatistic(api, [
      { shopCategoryName: shopModel.shopCategoryName, marketCategoryId: categoryData.hid, shopModelTotal: 1 },
    ]);

    await app.findByText(/Сейчас для этих товаров нельзя менять категорию/i);
  });

  test('empty models', async () => {
    const { app, api } = setupWithReatom(<TestApp route={UNCHANGEABLE_MODELS_URL} />);

    resolveStatistic(api, []);
    await app.findByText(/По выбранным фильтрам товаров не найдено/i);
  });

  test('load good categories', async () => {
    const { api } = setupWithReatom(<TestApp route={UNCHANGEABLE_MODELS_URL} />);

    act(() => {
      api.goodsGroupController.getGoodsGroups.next().resolve([goodCategoryGroup]);
    });

    await waitFor(() => expect(api.goodsGroupController.activeRequests()).toHaveLength(0));
  });
});
