import { render } from '@testing-library/react';

import { formatMarketCategoryOption, filterOptions } from './MarketCategoriesField';

const categoryOptions = {
  label: 'рули',
  value: 1,
  fullCategoryPath: 'аксесуары для приставок / рули',
};

const categoryOptionsTwo = {
  label: 'геймпады',
  value: 2,
  fullCategoryPath: 'аксесуары для приставок / геймпады',
};

const options = [categoryOptions, categoryOptionsTwo];

describe('MarketCategoriesField', () => {
  test('formatMarketCategoryOption show category hid', () => {
    const app = render(formatMarketCategoryOption(categoryOptions, true));

    app.getByText('рули #1');
  });

  test('formatMarketCategoryOption hidden category hid', () => {
    const app = render(formatMarketCategoryOption(categoryOptions, false));

    app.getByText('рули');
  });

  test('filterOptions search by category hid', () => {
    expect(filterOptions(options, '1')[0].value).toBe(1);
  });

  test('filterOptions search by category name and hid', () => {
    expect(filterOptions(options, 'рули 1')[0].value).toBe(1);
  });
});
