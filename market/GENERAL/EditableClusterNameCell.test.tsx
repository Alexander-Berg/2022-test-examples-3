import React from 'react';

import { EditableClusterNameCell, EditableClusterNameProps } from './EditableClusterNameCell';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { renderWithProvider } from 'src/test/utils/utils';

const cellProps: EditableClusterNameProps = { row: { id: 0 } } as EditableClusterNameProps;

describe('<EditableClusterNameCell/>', () => {
  it('renders without errors', () => {
    renderWithProvider(<EditableClusterNameCell {...cellProps} />, setupTestProvider());
  });
});
