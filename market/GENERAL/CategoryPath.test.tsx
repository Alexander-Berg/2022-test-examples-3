import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import R from 'ramda';

import { CategoryPath, UNKNOWN_CATEGORY } from './CategoryPath';
import { CATEGORIES } from 'src/test/data/shared/categories';

describe('CategoryPath', () => {
  const categoriesMap = R.indexBy(c => `${c.hid}`, CATEGORIES);
  const category = categoriesMap[90714];

  test('correct show category path', () => {
    render(<CategoryPath categoriesMap={categoriesMap} categoryId={category.hid} onSelect={jest.fn()} />);
    screen.getByText(categoriesMap[category.hid].name);
    screen.getByText(categoriesMap[category.parentHid].name);
  });

  test('select parent category', () => {
    const onSelect = jest.fn(id => {
      expect(id).toBe(category.parentHid);
    });
    render(<CategoryPath categoriesMap={categoriesMap} categoryId={category.hid} onSelect={onSelect} />);
    userEvent.click(screen.getByText(categoriesMap[category.parentHid].name));
  });

  test('empty categories', () => {
    render(<CategoryPath categoriesMap={{}} categoryId={category.hid} onSelect={jest.fn()} />);
    screen.getByText(new RegExp(UNKNOWN_CATEGORY));
  });

  test('without parents', () => {
    render(<CategoryPath categoriesMap={categoriesMap} categoryId={CATEGORIES[0].hid} onSelect={jest.fn()} />);
    screen.getByText(CATEGORIES[0].name);
  });
});
