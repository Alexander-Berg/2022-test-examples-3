import React from 'react';

import { SelectRowCell, SelectRowCellProps } from './SelectRowCell';
import { renderWithProvider } from 'src/test/utils/utils';
import { setupTestProvider } from 'src/test/setupTestProvider';

const cellProps: SelectRowCellProps = ({
  row: { index: 0, checked: false },
} as unknown) as SelectRowCellProps;

describe('<SelectRowCell/>', () => {
  it('renders without errors', () => {
    renderWithProvider(<SelectRowCell {...cellProps} />, setupTestProvider());
  });
});
