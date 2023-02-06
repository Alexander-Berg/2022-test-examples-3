import React from 'react';
import { render, waitFor } from '@testing-library/react';

import { OversizeValue } from './OversizeValue';
import userEvent from '@testing-library/user-event';

const fullSizeText =
  'нож кухонный стальной накири suncraft mu-08 овощной нож suncraft mu-08 внешним видом напоминает топорик и предназначен для рубки овощей и фруктов. имеет массивный';

describe('OversizeValue', () => {
  test('render', async () => {
    const app = render(<OversizeValue value={fullSizeText} />);
    app.getByText(new RegExp(fullSizeText));

    userEvent.click(app.getByTitle(/Показать все/i));

    await waitFor(() => {
      app.getAllByText(new RegExp(fullSizeText));
    });
  });
});
