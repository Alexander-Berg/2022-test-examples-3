import React from 'react';
import { render, screen } from '@testing-library/react';
import { PreviewMoney } from './PreviewMoney';

describe('PreviewMoney', () => {
  it('renders empty value', () => {
    render(<PreviewMoney />);

    expect(screen.getByText('–')).toBeInTheDocument();
  });

  it('renders empty price with 0', () => {
    render(<PreviewMoney value={{ currency: { id: 1, name: 'caption', code: 'code' } }} />);

    expect(screen.getByText('0 caption')).toBeInTheDocument();
  });

  it('renders empty currency label', () => {
    render(<PreviewMoney value={{ value: 100 }} />);

    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('renders full value', () => {
    render(
      <PreviewMoney value={{ value: 100, currency: { id: 1, name: 'caption', code: 'code' } }} />,
    );

    expect(screen.getByText('100 caption')).toBeInTheDocument();
  });
});
