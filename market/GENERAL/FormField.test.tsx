import React from 'react';
import { render, screen } from '@testing-library/react';

import { FormField } from './FormField';

describe('<FormField />', () => {
  it('should contains label', () => {
    render(<FormField label="Test label" />);

    expect(screen.getByText(/Test label/)).toBeInTheDocument();
  });

  it('should contains control', () => {
    render(<FormField>Control</FormField>);

    expect(screen.getByText(/Control/)).toBeInTheDocument();
  });
});
