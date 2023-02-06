import React from 'react';
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ExportExcelButton, LOAD_TEXT } from './ExportExcelButton';
import { shopModel, categoryData, shops } from 'src/test/data';
import { setupWithReatom } from 'src/test/withReatom';

describe('ExportExcelButton', () => {
  test('render', async () => {
    const { app } = setupWithReatom(
      <ExportExcelButton models={[shopModel]} currentCategoryName={categoryData.name} shopName={shops[0].name} />
    );

    userEvent.click(app.getByText(LOAD_TEXT));

    await waitFor(() => {
      app.getByText('Название файла');
      app.getByText('Данные');
    });
  });
});
