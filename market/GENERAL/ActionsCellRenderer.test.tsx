import React from 'react';
import { render } from '@testing-library/react';

import { ActionsCellRenderer } from './ActionsCellRenderer';

describe('<ActionsCellRenderer/>', () => {
  it('renders empty array without errors', () => {
    render(<ActionsCellRenderer value={[]} />);
  });

  it('renders without errors', () => {
    render(
      <ActionsCellRenderer
        value={[
          { name: '', callback: () => 1 },
          { name: '1', callback: () => 2 },
        ]}
      />
    );
  });
});
