import React from 'react';
import { render } from '@testing-library/react';

import { BooleanCheckboxRenderer } from './BooleanCheckboxRenderer';

describe('<BooleanCheckboxRenderer/>', () => {
  it('renders false without errors', () => {
    render(<BooleanCheckboxRenderer value={false} />);
  });

  it('renders true without errors', () => {
    render(<BooleanCheckboxRenderer value />);
  });
});
