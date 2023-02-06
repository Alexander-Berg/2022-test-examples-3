import React from 'react';
import userEvent from '@testing-library/user-event';
import { waitFor } from '@testing-library/react';

import { ExportExcelForm, SAVE_TEXT, DEFAULT_PARAMS } from './ExportExcelForm';
import { excelRowConfig } from './excel-rows';
import { setupWithReatom } from 'src/test/withReatom';
import { shopModel, categoryData, shops } from 'src/test/data';

describe.skip('ExportExcelForm', () => {
  window.URL.createObjectURL = jest.fn(() => 'filepath');
  test('render', async () => {
    const onClose = jest.fn();
    const { app } = setupWithReatom(
      <ExportExcelForm models={[shopModel]} categoryName={categoryData.name} shop={shops[0].name} onClose={onClose} />
    );

    // выбрана ли характеристика по умолчанию
    DEFAULT_PARAMS.forEach(el => app.getByText(excelRowConfig[el].label));

    userEvent.click(app.getByText(SAVE_TEXT));

    await waitFor(() => expect(onClose.mock.calls.length).toEqual(1));
  });
});
