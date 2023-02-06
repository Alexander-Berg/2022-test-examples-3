import React from 'react';

import { categoryInfo, shopModel, shops, userInfo } from 'src/test/data';
import { setupWithReatom } from 'src/test/withReatom';
import { CategoryRestrictionType, Role } from 'src/java/definitions';
import { goodCategoryGroupAtom } from 'src/store/categories/goodCategoryGroup.atom';
import { setShopIdAction, shopIdAtom, setShopListAction, shopsListAtom } from 'src/store/shop';
import { currentUserAtom, setCurrentUserAction } from 'src/store/user.atom';
import { TestingRouter } from 'src/test/setupApp';
import { CategoryItemActions } from './CategoryItemActions';
import { selectedModelsAtom } from 'src/store/shopModels';
import { act } from 'react-dom/test-utils';

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
  selectedModelsAtom,
  goodCategoryGroupAtom,
  shopsListAtom,
  currentUserAtom,
  shopIdAtom,
};

const defaultActions = [setShopListAction(shops), setShopIdAction(shops[0].id)];

const changeCategoryBtnText = new RegExp('Переместить в эту категорию');
const forcedCategoryBtnText = new RegExp('Форсировать категорию', 'i');

describe('CategoryTreeItemDetails', () => {
  const callYm = jest.fn();
  window.ym = callYm;

  test('Не показываем кнопку изменения категории когда фильтр canChangeCategory = false', async () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=0">
        <CategoryItemActions category={categoryInfo} selectedModels={[shopModel]} />
      </TestingRouter>,
      defaultAtoms,
      defaultActions
    );

    expect(app.queryByText(changeCategoryBtnText)).toBeFalsy();
  });

  test('Показываем кнопку изменения категории, когда canChangeCategory = true', async () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=1">
        <CategoryItemActions category={categoryInfo} selectedModels={[shopModel]} />
      </TestingRouter>,
      defaultAtoms,
      defaultActions
    );

    app.getByText(changeCategoryBtnText);
  });

  test('Показываем кнопку форсирования категории, когда не можем сделать простую смену категории + role === Role.OPERATOR || Role.ADMIN', async () => {
    const { app, reatomStore } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=1">
        <CategoryItemActions category={categoryInfo} selectedModels={[modelWithGoodGroup]} />
      </TestingRouter>,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.ADMIN })]
    );

    app.getByText(forcedCategoryBtnText);

    // если OPERATOR
    act(() => {
      reatomStore.dispatch(setCurrentUserAction({ ...userInfo, role: Role.OPERATOR }));
    });
    app.getByText(forcedCategoryBtnText);
  });

  test('Не показываем кнопку изменения категории когда acceptGoodContent === false', async () => {
    const { app, reatomStore } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=1">
        <CategoryItemActions
          category={{ ...categoryInfo, acceptGoodContent: false }}
          selectedModels={[modelWithGoodGroup]}
        />
      </TestingRouter>,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo })]
    );

    expect(app.queryByText(changeCategoryBtnText)).toBeFalsy();

    // если OPERATOR или ADMIN , то показываем форсирование категории
    act(() => {
      reatomStore.dispatch(setCurrentUserAction({ ...userInfo, role: Role.OPERATOR }));
    });
    app.getByText(forcedCategoryBtnText);

    act(() => {
      reatomStore.dispatch(setCurrentUserAction({ ...userInfo, role: Role.ADMIN }));
    });
    app.getByText(forcedCategoryBtnText);
  });
});
