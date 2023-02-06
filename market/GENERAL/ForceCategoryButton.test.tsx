import React from 'react';
import { RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { setupWithReatom } from 'src/test/withReatom';
import { categoryInfo, shopModel, shops, userInfo } from 'src/test/data';
import { Role } from 'src/java/definitions';
import { ForceCategoryButton } from './ForceCategoryButton';
import { currentUserAtom, setCurrentUserAction } from 'src/store/user.atom';
import { setShopIdAction, shopIdAtom, setShopListAction, shopsListAtom } from 'src/store/shop';
import { Api } from 'src/java/Api';

const defaultAtoms = {
  shopsListAtom,
  currentUserAtom,
  shopIdAtom,
};

const defaultActions = [setShopListAction(shops), setShopIdAction(shops[0].id)];

const forcedCategoryBtnText = new RegExp('Форсировать категорию', 'i');

const forceCategoryTest = (app: RenderResult, api: MockedApiObject<Api>) => {
  const btn = app.getByText(forcedCategoryBtnText);

  userEvent.click(btn);

  const res = { forcedCategoryId: categoryInfo.hid, shopId: shops[0].id, shopSkus: [shopModel.shopSku] };
  expect(api.forceCategoryController.forceCategory.activeRequests()[0][0]).toEqual(res);
};

describe('<ForceCategoryButton />', () => {
  test('Принудительно меняем категорию когда user = ADMIN', async () => {
    const { app, api } = setupWithReatom(
      <ForceCategoryButton models={[shopModel]} category={categoryInfo} />,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.ADMIN })]
    );
    // у ADMIN или OPERATOR должна быть возможноть принудительно ппоменть категорию у этого товара
    forceCategoryTest(app, api);
  });

  test('Принудительно меняем категорию когда user = OPERATOR', async () => {
    const { app, api } = setupWithReatom(
      <ForceCategoryButton models={[shopModel]} category={categoryInfo} />,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.OPERATOR })]
    );

    forceCategoryTest(app, api);
  });

  test('Не отображать форсирование категории у обычного пользователя user = MANAGER', () => {
    const { app } = setupWithReatom(
      <ForceCategoryButton models={[shopModel]} category={categoryInfo} />,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.MANAGER })]
    );
    expect(app.queryByText(forcedCategoryBtnText)).toBeFalsy();
  });
});
