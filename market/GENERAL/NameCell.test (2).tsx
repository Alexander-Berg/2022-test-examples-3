import React from 'react';
import { render, screen } from '@testing-library/react';

import { FastFilter } from 'src/java/definitions';
import { NameCell } from './NameCell';

const fastFilter = {
  name: 'foo',
} as FastFilter;

describe('<NameCell />', () => {
  it('render simple', () => {
    render(<NameCell {...fastFilter} />);

    const name = screen.getByText('foo');

    expect(name).toBeInTheDocument();
  });
});
