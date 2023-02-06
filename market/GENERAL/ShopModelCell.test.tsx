import React from 'react';
import { fireEvent } from '@testing-library/react';

import { shopModel } from 'src/test/data';
import { setupWithReatom } from 'src/test/withReatom';
import { ShopModelCell } from './ShopModelCell';

describe('ShopModelCell', () => {
  test('correct render', () => {
    const { app } = setupWithReatom(<ShopModelCell row={shopModel} />);
    const text = app.getByText(shopModel.name);
    // show details
    fireEvent.click(text);
    app.getAllByText(new RegExp(shopModel.description!.substring(0, 10)));
  });

  test('render without name', () => {
    const { app } = setupWithReatom(<ShopModelCell row={shopModel} hiddenName />);
    expect(app.queryByText(shopModel.name)).toBeFalsy();
  });
});
