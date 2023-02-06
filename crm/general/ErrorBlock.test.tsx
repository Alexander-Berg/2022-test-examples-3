import React from 'react';
import { render, screen } from '@testing-library/react';
import { ErrorBlock } from './ErrorBlock';

describe('ErrorBlock', () => {
  it('renders error by error object', () => {
    render(<ErrorBlock error={new Error('error')} />);

    expect(screen.getByText(/Error: error/)).toBeInTheDocument();
  });

  it('renders error by message', () => {
    render(<ErrorBlock message="error" />);

    expect(screen.getByText(/Error: error/)).toBeInTheDocument();
  });

  it('renders error by children', () => {
    render(<ErrorBlock>error</ErrorBlock>);

    expect(screen.getByText(/Error: error/)).toBeInTheDocument();
  });
});
