import React from 'react';
import { render, screen } from '@testing-library/react';

import { FormField } from './FormField';

describe('<FormField />', () => {
  it('renders without errors', () => {
    render(<FormField label="Test label" />);

    expect(screen.getByText(/Test label/)).toBeInTheDocument();
  });
});
