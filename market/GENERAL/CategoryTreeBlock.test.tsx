import React from 'react';
import userEvent from '@testing-library/user-event';
import { RenderResult } from '@testing-library/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { setupWithReatom } from 'src/test/withReatom';
import { TestingRouter } from 'src/test/setupApp';
import { shopModel, shops, userInfo } from 'src/test/data';
import { setShopIdAction, shopIdAtom, setShopListAction, shopsListAtom } from 'src/store/shop';
import { currentUserAtom, setCurrentUserAction } from 'src/store/user.atom';
import { CategoryTreeBlock } from './CategoryTreeBlock';
import { Role } from 'src/java/definitions';
import { Api } from 'src/java/Api';

const defaultAtoms = {
  shopsListAtom,
  currentUserAtom,
  shopIdAtom,
};

const defaultActions = [setShopListAction(shops), setShopIdAction(shops[0].id)];

const forceClassificationBtnText = new RegExp('Пожаловаться на классификацию', 'i');

const testForceClassification = (app: RenderResult, api: MockedApiObject<Api>) => {
  userEvent.click(app.getByText(forceClassificationBtnText));

  const request = { shopId: shops[0].id, shopSkus: [shopModel.shopSku] };
  expect(api.forceClassificationController.forceClassification.activeRequests()[0][0]).toEqual(request);
};

describe('CategoryTreeBlock', () => {
  test('force Classification ADMIN canChangeCategory = false', () => {
    const resetSelection = jest.fn();
    const { app, api } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=0">
        <CategoryTreeBlock selectedModels={[shopModel]} onResetSelection={resetSelection} />
      </TestingRouter>,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.ADMIN })]
    );

    testForceClassification(app, api);
  });

  test('force Classification ADMIN canChangeCategory = true', () => {
    const resetSelection = jest.fn();
    const { app, api } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=1">
        <CategoryTreeBlock selectedModels={[shopModel]} onResetSelection={resetSelection} />
      </TestingRouter>,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.ADMIN })]
    );

    testForceClassification(app, api);
  });

  test('force Classification OPERATOR canChangeCategory = false', () => {
    const resetSelection = jest.fn();
    const { app, api } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=0">
        <CategoryTreeBlock selectedModels={[shopModel]} onResetSelection={resetSelection} />
      </TestingRouter>,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.OPERATOR })]
    );

    testForceClassification(app, api);
  });

  test('force Classification MANAGER canChangeCategory = false', () => {
    const resetSelection = jest.fn();
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=0">
        <CategoryTreeBlock selectedModels={[shopModel]} onResetSelection={resetSelection} />
      </TestingRouter>,
      defaultAtoms,
      [...defaultActions, setCurrentUserAction({ ...userInfo, role: Role.MANAGER })]
    );

    expect(app.queryByText(forceClassificationBtnText)).toBeFalsy();
  });
});
