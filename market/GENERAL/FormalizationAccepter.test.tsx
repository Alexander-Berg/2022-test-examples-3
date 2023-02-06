import React from 'react';
import userEvent from '@testing-library/user-event';
import { waitFor } from '@testing-library/react';

import { shopModel, parameter, formalizationValues, categoryData } from 'src/test/data';
import { FormalizationAccepter } from './FormalizationAccepter';
import { setupWithReatom } from 'src/test/withReatom';
import { resolveSaveModelsRequest } from 'src/test/api/resolves';
import { categoryDataAtom, setCategoryData } from 'src/store/categories';
import { currentCategoryIdAtom } from '../../store';
import { filterAtom, setFilterAction } from '../../store/filter.atom';
import { setAllShopModelsAction, shopModelsAtom } from 'src/store/shopModels';

const defaultAtoms = { categoryDataAtom, currentCategoryIdAtom, filterAtom, shopModelsAtom };
const defaultActions = [
  setCategoryData(categoryData),
  setFilterAction({ marketCategoryId: categoryData.hid }),
  setAllShopModelsAction([shopModel]),
];

describe('FormalizationAccepter', () => {
  test('accept values', async () => {
    const { app, api } = setupWithReatom(
      <FormalizationAccepter model={shopModel} parameter={parameter} values={formalizationValues} />,
      defaultAtoms,
      defaultActions
    );

    const btn = app.getByTitle(/Подтвердить значения/);
    userEvent.click(btn);

    await waitFor(() => {
      resolveSaveModelsRequest(api, [shopModel]);
    });
  });
});
