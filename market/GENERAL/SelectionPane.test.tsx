import React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { SelectionPane, getSelectionInfo } from './SelectionPane';

test('render SelectionPane without selection', () => {
  const onReset = jest.fn();
  const app = render(<SelectionPane selectedCount={0} total={10} onResetSelected={onReset} />);
  app.getByText(/Всего 10/i);
});

test('render SelectionPane with selection', () => {
  const onReset = jest.fn();
  const app = render(<SelectionPane selectedCount={5} total={10} onResetSelected={onReset} />);
  app.getByText(getSelectionInfo(5, 10));

  // сбрасываем выбранное
  const resetBtn = app.getByText('Сбросить');
  fireEvent.click(resetBtn);

  expect(onReset.mock.calls.length).toEqual(1);
});
