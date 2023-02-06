import React from 'react';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Action } from '@reatom/core';

import { CategoryChanger } from './CategoryChanger';
import { setupWithReatom } from 'src/test/withReatom';
import { categoryInfo, shopModel, categoryData } from 'src/test/data';
import { setGoodCategoryGroupsAction, goodCategoryGroupAtom } from 'src/store/categories/goodCategoryGroup.atom';
import { resolveLoadCategoryDataRequest } from 'src/test/api/resolves';
import { CategoryRestrictionType } from 'src/java/definitions';
import { HAS_RESTRICTED_MODELS_TEXT, CANCEL_RESTRICTED_MODELS_TEXT } from './RestrictedCategoryModels';

const goodCategoryId = 5;

const goodCategoryGroup = {
  categoryIds: [goodCategoryId],
  id: 1,
  name: 'Text category',
};

const modelWithGoodGroup = {
  ...shopModel,
  id: 5,
  marketCategoryId: goodCategoryId,
  categoryRestriction: {
    type: CategoryRestrictionType.ALLOW_GROUP,
    categoryId: shopModel.marketCategoryId,
    goodsGroupId: goodCategoryGroup.id,
  },
};

const defaultAtoms = {
  goodCategoryGroupAtom,
};

const defaultActions: Action<any>[] = [];

const changeCategoryBtnText = new RegExp('Переместить в эту категорию');

describe('<CategoryChanger />', () => {
  test('Меняем категорию у товара', async () => {
    const { app, api } = setupWithReatom(
      <CategoryChanger category={categoryInfo} models={[shopModel]} onCancelModels={jest.fn()} />,
      defaultAtoms,
      defaultActions
    );

    userEvent.click(app.getByText(changeCategoryBtnText));

    resolveLoadCategoryDataRequest(api, categoryData);

    await waitFor(() => expect(api.shopModelController.updateModelsV2.activeRequests()).toHaveLength(0));
  });

  test('Перекладывание товара с чз категорией не должно быть доступно если выбраная категория не входит в группу каетегорий товара', async () => {
    const { app, api } = setupWithReatom(
      <CategoryChanger category={categoryInfo} models={[modelWithGoodGroup]} onCancelModels={jest.fn()} />,
      defaultAtoms,
      [...defaultActions, setGoodCategoryGroupsAction({ [goodCategoryGroup.id]: goodCategoryGroup })]
    );

    app.getByText(new RegExp(HAS_RESTRICTED_MODELS_TEXT, 'i'));

    // при клике не должен уходить запрос на смену, так как товар нельзя перекладывать в выбраную категорию
    userEvent.click(app.getByText(changeCategoryBtnText));
    expect(api.allActiveRequests).toEqual({});
  });

  test('Изменение категории в товаре когда по какой то причине не пришел список гуд категорий', async () => {
    const { app, api } = setupWithReatom(
      <CategoryChanger category={categoryInfo} models={[modelWithGoodGroup]} onCancelModels={jest.fn()} />,
      defaultAtoms,
      defaultActions
    );

    app.getByText(new RegExp(HAS_RESTRICTED_MODELS_TEXT, 'i'));

    // при клике не должен уходить запрос на смену, так как нет списка гуд категорий хотя в товаре она указана
    userEvent.click(app.getByText(changeCategoryBtnText));
    expect(api.allActiveRequests).toEqual({});
  });

  test('Перекладывание товара внутри одной чз группы должно быть доступно', async () => {
    const { app, api } = setupWithReatom(
      <CategoryChanger category={categoryInfo} models={[modelWithGoodGroup]} onCancelModels={jest.fn()} />,
      defaultAtoms,
      [
        ...defaultActions,
        // кладем категорию которая входит в группу категори товара
        setGoodCategoryGroupsAction({
          [goodCategoryGroup.id]: { ...goodCategoryGroup, categoryIds: [goodCategoryId, categoryInfo.hid] },
        }),
      ]
    );

    expect(app.queryByText(new RegExp(HAS_RESTRICTED_MODELS_TEXT, 'i'))).toBeFalsy();

    userEvent.click(app.getByText(changeCategoryBtnText));

    resolveLoadCategoryDataRequest(api, categoryData);

    await waitFor(() => expect(api.shopModelController.updateModelsV2.activeRequests()).toHaveLength(0));
  });

  test('Сбрасываем товары у которых нельзя сменить категорию', async () => {
    const onDeselectModels = jest.fn(model => {
      expect(model[0].id).toBe(modelWithGoodGroup.id);
    });

    const { app } = setupWithReatom(
      <CategoryChanger
        category={categoryInfo}
        models={[modelWithGoodGroup, shopModel]}
        onCancelModels={onDeselectModels}
      />,
      defaultAtoms,
      [...defaultActions, setGoodCategoryGroupsAction({ [goodCategoryGroup.id]: goodCategoryGroup })]
    );

    userEvent.click(app.getByText(CANCEL_RESTRICTED_MODELS_TEXT));

    expect(onDeselectModels).toHaveBeenCalledTimes(1);
  });
});
