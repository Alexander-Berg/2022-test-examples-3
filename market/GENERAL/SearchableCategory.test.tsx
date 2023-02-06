import { render } from '@testing-library/react';
import React from 'react';
import userEvent from '@testing-library/user-event';

import { SearchableCategory } from './SearchableCategory';
import { categoryInfo } from 'src/test/data';
import { CategoryColumn } from '../CategoryTreeItem';

export const categoryStatColumns: CategoryColumn[] = [
  {
    title: 'Товаров в категории',
    key: 'total',
    formatter: (value: any) => value.hid,
  },
];

describe('SearchableCategory', () => {
  test('render/select', () => {
    const onSelect = jest.fn();
    const app = render(
      <SearchableCategory category={categoryInfo} columns={categoryStatColumns} onSelect={onSelect} />
    );

    app.getByText(categoryInfo.name);
    app.getByText(categoryInfo.fullName);

    userEvent.click(app.getByText(/выбрать/i));

    expect(onSelect.mock.calls.length).toEqual(1);
  });
});
