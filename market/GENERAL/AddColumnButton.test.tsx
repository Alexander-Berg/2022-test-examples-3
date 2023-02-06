import React from 'react';
import { render } from '@testing-library/react';

import { AddColumnButton } from './AddColumnButton';

describe('<AddColumnButton />', () => {
  it('renders without errors', () => {
    render(<AddColumnButton usedMeasured={[]} visibleColumns={[]} hiddenColumns={[]} onAddColumns={jest.fn()} />);
  });
});
