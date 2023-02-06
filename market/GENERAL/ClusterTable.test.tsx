import React from 'react';

import { setupTestProvider } from 'src/test/setupTestProvider';
import { ClusterTable, ClusterTableProps } from './ClusterTable';
import { renderWithProvider } from 'src/test/utils/utils';

const defaultProps: ClusterTableProps = {
  onSortChange: jest.fn,
  categoryId: 0,
};

describe('<ClusterTable />', () => {
  it('renders without errors', () => {
    renderWithProvider(<ClusterTable {...defaultProps} />, setupTestProvider());
  });
});
