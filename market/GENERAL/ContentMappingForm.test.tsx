import React from 'react';
import { act, fireEvent, RenderResult, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { shopModel, simpleMapping, simpleMappingWithRule } from 'src/test/data';
import { categoryData, categoryStat, categoryInfo, parameter } from 'src/test/data/categoryData';
import { setupWithReatom } from 'src/test/withReatom';
import { ContentMappingForm, ADD_MAPPING_TEXT, EXIST_MAPPING_ERROR, saveModeOptions } from './ContentMappingForm';
import { resolveFormalizeMappingRequest, resolveSaveMappingRequest } from 'src/test/api/resolves';
import { setAllShopModelsAction, shopModelsAtom } from 'src/store/shopModels';
import { currentCategoryDataAtom, currentCategoryIdAtom } from 'src/pages/ParameterSetting/store';
import {
  categoriesStatMapAtom,
  setCategoryStatisticAction,
  categoryStatisticsAtom,
  categoryDataAtom,
  setCategoryData,
  categoryTreeAtom,
  setCategoryTreeAction,
} from 'src/store/categories';
import { UiCategoryStat } from 'src/utils/types';
import { filterAtom, setFilterAction } from 'src/pages/ParameterSetting/store/filter.atom';
import { Api } from 'src/java/Api';
import {
  paramMappingsAtom,
  updateParamMappingsAction,
} from 'src/pages/ParameterSetting/store/mappings/paramMappings.atom';
import { createMapping } from 'src/entities/mapping/utils';
import { getCategoryInfoMap } from 'src/entities/category';

const typeSelect = async (app: RenderResult, inputs: HTMLElement, value: string, optionDisplay: string) => {
  userEvent.type(inputs, value);
  const option = await app.findAllByText(new RegExp(optionDisplay));
  fireEvent.keyDown(option[1] || option[0], { key: 'Enter', code: 'Enter' });
  userEvent.type(inputs, value);
};

const parentCategory = {
  ...categoryInfo,
  ...categoryStat,
  hid: 1,
  parentHid: 0,
  name: 'Приготовление пищи',
  isLeaf: false,
};

const leafCategory = {
  ...categoryInfo,
  ...categoryStat,
  total: 1,
  hid: 2,
  parentHid: 1,
  name: 'Ножи кухонные',
  isLeaf: true,
};

const leafCategoryTwo = {
  ...leafCategory,
  hid: 3,
  name: 'Наборы ножей',
};

const categoriesStat: UiCategoryStat[] = [parentCategory, leafCategory, leafCategoryTwo];

const defaultAtoms = {
  shopModelsAtom,
  categoryDataAtom,
  currentCategoryIdAtom,
  categoryStatisticsAtom,
  currentCategoryDataAtom,
  categoriesStatMapAtom,
  filterAtom,
  categoryTreeAtom,
  paramMappingsAtom,
};

const defaultActions = [
  setAllShopModelsAction([{ ...shopModel, marketCategoryId: 2 }]),
  setFilterAction({ marketCategoryId: 1 }),
  setCategoryData({ ...categoryData, hid: 1, leaf: false, name: parentCategory.name }),
  setCategoryStatisticAction(categoriesStat),
  setCategoryTreeAction(getCategoryInfoMap(categoriesStat)),
];

const checkResolveRequest = async (api: MockedApiObject<Api>) => {
  const response = { response: { paramMapping: simpleMapping, rules: [] }, updatedModels: [shopModel] };
  resolveSaveMappingRequest(api, response);
  await waitFor(() => {
    expect(api.allActiveRequests).toEqual({});
  });
};

const changeMappingCategory = async (app: RenderResult, category: UiCategoryStat) => {
  const inputs = app.getAllByRole('textbox');
  await typeSelect(app, inputs[4], category.hid.toString(), category.name);
  await app.findAllByText(new RegExp(category.name));
};

const clickSave = (app: RenderResult) => {
  userEvent.click(app.getByText(/сохранить/i));
};

describe('ContentMappingForm', () => {
  const callYm = jest.fn();
  window.ym = callYm;

  test('correct render', () => {
    const onClose = jest.fn();
    const { app } = setupWithReatom(
      <ContentMappingForm mappings={[simpleMapping]} onCancel={onClose} />,
      defaultAtoms,
      defaultActions
    );

    // отображается ли параметр магазина
    app.getAllByText(new RegExp(simpleMapping.shopParams[0].name, 'i'));
  });

  test('save mapping', async () => {
    const onClose = jest.fn();
    const { app, api } = setupWithReatom(
      <ContentMappingForm mappings={[simpleMapping]} onCancel={onClose} />,
      defaultAtoms,
      defaultActions
    );

    clickSave(app);

    const response = { response: { paramMapping: simpleMapping, rules: [] }, updatedModels: [shopModel] };
    resolveSaveMappingRequest(api, response);
    expect(api.allActiveRequests).toEqual({});

    await waitFor(() => expect(onClose).toBeCalledTimes(1));
  });

  test('save new mapping', async () => {
    const onClose = jest.fn();
    const newMapping = createMapping(parameter.id, simpleMapping.shopId);
    newMapping.shopParams = simpleMapping.shopParams;

    const { app, api } = setupWithReatom(
      <ContentMappingForm mappings={[newMapping]} onCancel={onClose} />,
      defaultAtoms,
      defaultActions
    );

    clickSave(app);
    // проверяем и резолвим запрос на сохранение маппинга
    expect(api.paramMappingControllerV2.saveParamMappingWithRulesDiff).toBeCalledTimes(1);
    const response = { response: { paramMapping: newMapping, rules: [] }, updatedModels: [shopModel] };
    resolveSaveMappingRequest(api, response);
    expect(api.paramMappingControllerV2.saveParamMappingWithRulesDiff.activeRequests()).toHaveLength(0);
    // после сохранения нового маппинга должен отработать поиск гипотез
    await waitFor(() => expect(api.formalizerController.formalizeAndUpdateMapping).toBeCalledTimes(1));
    const formalizeResponse = { allParamMappingRules: [], updatedModels: [] };
    resolveFormalizeMappingRequest(api, formalizeResponse);

    await waitFor(() => expect(onClose).toBeCalledTimes(1));
  });

  test('add mapping', async () => {
    const onClose = jest.fn();
    const { app } = setupWithReatom(
      <ContentMappingForm mappings={[simpleMapping]} disabledMarketParams onCancel={onClose} />,
      defaultAtoms,
      defaultActions
    );

    userEvent.click(app.getByText(ADD_MAPPING_TEXT));

    const deleteButtons = app.getAllByTitle('delete');

    expect(deleteButtons.length).toBe(2);

    userEvent.click(deleteButtons[1]);

    expect(app.getAllByTitle('delete').length).toBe(1);
  });

  test('copy mapping with rules', async () => {
    const onClose = jest.fn();
    const { app, api } = setupWithReatom(
      <ContentMappingForm mappings={[simpleMappingWithRule]} disabledMarketParams onCancel={onClose} />,
      defaultAtoms,
      defaultActions
    );

    // выбираем листовую категорию
    await changeMappingCategory(app, leafCategory);
    // // включаем режим копирования
    userEvent.click(app.getByText(saveModeOptions[1].label));
    userEvent.click(await app.findByText('Копировать'));

    const response = { response: { paramMapping: simpleMapping, rules: [] }, updatedModels: [shopModel] };

    // сохраняем маппинг что бы получить его ид
    act(() => {
      api.paramMappingControllerV2.saveParamMappingWithRulesDiff
        .next(req => req.paramMapping.categoryId === 2 && req.paramMapping.id === 0)
        .resolve(response);
    });

    await waitFor(() => {
      api.paramMappingControllerV2.saveParamMappingWithRulesDiff
        .next(req => req.rulesToAdd.length > 0)
        .resolve(response);
    });

    await waitFor(() => expect(onClose).toBeCalledTimes(1));
  });

  test('save mapping with parent category to leaf category', async () => {
    const onClose = jest.fn();
    const confirm = jest.fn(() => true);
    window.confirm = confirm;

    const { app, api } = setupWithReatom(
      <ContentMappingForm mappings={[simpleMappingWithRule]} disabledMarketParams onCancel={onClose} />,
      defaultAtoms,
      defaultActions
    );

    // выбираем листовую категорию
    await changeMappingCategory(app, leafCategory);

    clickSave(app);

    // так как проставляем листовую категорию должна выскакивать предупреждалка
    expect(confirm).toBeCalledTimes(1);

    await checkResolveRequest(api);
  });

  test('save mapping with leaf to leaf category', async () => {
    const onClose = jest.fn();

    const { app, api } = setupWithReatom(
      <ContentMappingForm
        mappings={[{ ...simpleMappingWithRule, categoryId: leafCategory.hid }]}
        disabledMarketParams
        onCancel={onClose}
      />,
      defaultAtoms,
      defaultActions
    );

    // отображается ли категория
    await app.findByText(new RegExp(leafCategory.name));

    // выбираем другую листовую категорию
    await changeMappingCategory(app, leafCategoryTwo);

    clickSave(app);

    await checkResolveRequest(api);

    expect(onClose).toBeCalledTimes(1);
  });

  test('try save same mapping', async () => {
    const onClose = jest.fn();

    const { app, api } = setupWithReatom(
      <ContentMappingForm
        mappings={[{ ...simpleMappingWithRule, id: 1, categoryId: leafCategory.hid }]}
        disabledMarketParams
        onCancel={onClose}
      />,
      defaultAtoms,
      [
        ...defaultActions,
        updateParamMappingsAction([{ ...simpleMappingWithRule, id: 2, categoryId: leafCategoryTwo.hid }]),
      ]
    );

    // выбираем категорию для которой уже есть похожий маппинг
    await changeMappingCategory(app, leafCategoryTwo);
    // должна отобразиться ошибка
    await app.findByText(EXIST_MAPPING_ERROR);

    // кнопка сохранения должна быть заблокирована
    clickSave(app);
    expect(api.allActiveRequests).toEqual({});
  });

  test('save new mapping with category when exist such mapping for all category', async () => {
    const onClose = jest.fn();
    const confirm = jest.fn(() => true);
    window.confirm = confirm;

    const { app, api } = setupWithReatom(
      <ContentMappingForm mappings={[{ ...simpleMappingWithRule, id: 0 }]} disabledMarketParams onCancel={onClose} />,
      { ...defaultAtoms, paramMappingsAtom },
      [...defaultActions, updateParamMappingsAction([simpleMappingWithRule])]
    );

    // выбираем листовую категорию
    await changeMappingCategory(app, leafCategory);

    clickSave(app);

    // так как проставляем листовую категорию должна выскакивать предупреждалка
    expect(confirm).toBeCalledTimes(0);

    await checkResolveRequest(api);
  });

  test('dont show copy mode when create new mapping', async () => {
    const { app } = setupWithReatom(
      <ContentMappingForm
        mappings={[{ ...simpleMappingWithRule, id: -1 }]}
        disabledMarketParams
        onCancel={jest.fn()}
      />,
      defaultAtoms,
      defaultActions
    );

    // не должен отображаться выбор коппирования маппинга
    expect(app.queryByText(saveModeOptions[1].label)).not.toBeInTheDocument();
  });
});
