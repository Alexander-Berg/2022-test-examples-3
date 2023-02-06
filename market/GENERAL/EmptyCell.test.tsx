import React from 'react';
import { screen } from '@testing-library/react';

import { renderTableCell } from 'src/test/setupTestProvider';
import { EmptyCell } from '.';

describe('<EmptyCell />', () => {
  it('should be shown default content', () => {
    renderTableCell(<EmptyCell />);

    expect(screen.getByText(/Нет данных/i)).toBeInTheDocument();
  });

  it('should be shown custom content', () => {
    renderTableCell(<EmptyCell>Empty data</EmptyCell>);

    expect(screen.getByText(/Empty data/i)).toBeInTheDocument();
  });
});
