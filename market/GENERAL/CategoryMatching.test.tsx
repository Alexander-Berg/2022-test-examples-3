/* eslint-disable @typescript-eslint/no-unused-vars */
import { render, screen } from '@testing-library/react';
import React from 'react';

import { CategoryMatching } from './CategoryMatching';
import { category } from './data';
import { renderWithProvider } from 'src/test/setupTestProvider';

describe('CategoryMatching', () => {
  test('<CategoryMatching />', () => {
    renderWithProvider(<CategoryMatching hid={category.hid} />);
    screen.getByText('Загрузка данных...');
    // todo - как будет ручка сохранения проверить заполнение данных через форму
    // api.catalogEditController.getEditData.next().resolve(matchedCategoryProperties);
  });
});
