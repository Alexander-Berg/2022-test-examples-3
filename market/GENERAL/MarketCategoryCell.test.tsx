import React, { FC } from 'react';
import { fireEvent, waitFor } from '@testing-library/react';

import { shopModel, agValidations } from 'src/test/data/shopModel';
import { MarketCategoryCell, MarketCategoryCellProps } from './MarketCategoryCell';
import { setupWithReatom, useDescribeCategoryTree } from 'src/test/withReatom';
import { categoryInfo } from 'src/test/data';

const TestApp: FC<MarketCategoryCellProps> = ({ row }) => {
  useDescribeCategoryTree({ [categoryInfo.hid]: categoryInfo });
  return <MarketCategoryCell row={row} />;
};

describe('MarketCategoryCell', () => {
  test('render', () => {
    const { app } = setupWithReatom(<TestApp row={shopModel} />);
    app.getByText(new RegExp(categoryInfo.name, 'i'));
  });

  test('render not valid model', async () => {
    const invalidModel = {
      ...shopModel,
      validationResult: agValidations,
    };
    const { app } = setupWithReatom(<TestApp row={invalidModel} />);
    const text = app.getByText(new RegExp(categoryInfo.name, 'i'));

    await waitFor(() => {
      const hasErrorCls = app.container.querySelector('.ErrorCell');
      expect(hasErrorCls).toBeInTheDocument();

      fireEvent.mouseOver(text);
      app.getByText(new RegExp(agValidations.errors[0].message, 'i'));
    });
  });
});
