import React from 'react';
import { render, screen } from '@testing-library/react';

import { SimpleCellModal } from './SimpleCellModal';
import { GridCellParams } from '@material-ui/data-grid';
import userEvent from '@testing-library/user-event';

describe('<GridCellModal/>', () => {
  it('shows modal with text', async () => {
    const value: GridCellParams = {
      value: 'Long test provided to by Qwerty Qwertievna & Testik Testovich',
    } as GridCellParams;

    Object.defineProperty(HTMLElement.prototype, 'scrollHeight', { configurable: true, value: 500 });

    const app = render(
      <div style={{ width: 30 }}>
        <SimpleCellModal {...value} />
      </div>
    );

    const cell = await app.findByText('Qwerty Qwertievna', { exact: false });

    userEvent.click(cell);

    let allCellValues = await screen.findAllByText(value.value! as string);
    expect(allCellValues).toHaveLength(2);

    userEvent.click(cell.parentElement!);

    allCellValues = await screen.findAllByText(value.value! as string);
    expect(allCellValues).toHaveLength(1);
  });

  it('shows modal with highlighted sql', async () => {
    const value: GridCellParams = {
      value: 'use test; SELECT * FROM "table_of_qwertys"',
    } as GridCellParams;

    Object.defineProperty(HTMLElement.prototype, 'scrollHeight', { configurable: true, value: 500 });

    const app = render(
      <div style={{ width: 30 }}>
        <SimpleCellModal {...value} />
      </div>
    );

    const cell = app.getByText('use test;', { exact: false });

    userEvent.click(cell);

    const allCellValues = await screen.findAllByText('table_of_qwertys', { exact: false });
    expect(allCellValues).toHaveLength(2);
  });
});
