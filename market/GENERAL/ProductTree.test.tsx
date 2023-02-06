import React from 'react';
import { act, screen } from '@testing-library/react';

import { renderWithProvider } from 'src/test/setupTestProvider';
import { ProductTreePage } from './ProductTree';
import { api } from 'src/test/singletons/apiSingleton';
import { CATEGORIES } from 'src/test/data';

describe('ProductTreePage::', () => {
  test('correct render', async () => {
    renderWithProvider(<ProductTreePage />);

    await act(async () => {
      await api.categoryTreeController.getCategories.next().resolve(CATEGORIES);
    });

    screen.getByText('Все товары');
  });
});
