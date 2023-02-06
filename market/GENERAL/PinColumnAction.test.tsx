import React from 'react';

import { setupWithReatom } from 'src/test/withReatom';
import { PinColumnAction, PIN_COLUMN_TITLE } from './PinColumnAction';
import userEvent from '@testing-library/user-event';
import { pinnedColumnsAtom } from '../../store/pinnedColumnsAtom';
import { act } from '@testing-library/react';

describe('<PinColumnAction />', () => {
  test('pin column', () => {
    const { app, reatomStore } = setupWithReatom(<PinColumnAction columnKey="column-key" />);
    const pinBtn = app.getByTitle(PIN_COLUMN_TITLE);

    // закрепляем колонку
    act(() => {
      userEvent.click(pinBtn);
    });
    expect(reatomStore.getState(pinnedColumnsAtom).get('column-key')).toBeTruthy();

    // открепляем
    act(() => {
      userEvent.click(pinBtn);
    });
    expect(reatomStore.getState(pinnedColumnsAtom).get('column-key')).toBeFalsy();
  });
});
