import React from 'react';
import { render, screen } from '@testing-library/react';
import { UnknownField } from './UnknownField';

describe('UnknownField', () => {
  it('renders unknown field type message', () => {
    render(<UnknownField field={{ type: 'text' }} />);

    expect(screen.getByText(/Unknown block with type: text/)).toBeInTheDocument();
  });
});
