import React from 'react';
import { render, screen } from '@testing-library/react';

import { InheritValuesList } from './InheritValuesList';

describe('<InheritValuesList />', () => {
  it('renders without errors', () => {
    const name = 'name';
    const value = 'value';

    const data = {
      name,
      id: 0,
      value,
    };

    render(<InheritValuesList data={data} />);
    expect(screen.getByText(name)).not.toBeNull();
    expect(screen.getByText(value)).not.toBeNull();
  });
});
