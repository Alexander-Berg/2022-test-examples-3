import React from 'react';
import { render, screen } from '@testing-library/react';

import { FastFilter } from 'src/java/definitions';
import { PublishedCell } from './PublishedCell';

const publishedFastFilter = {
  is_published: true,
} as FastFilter;

const unpublishedFastFilter = {
  is_published: false,
} as FastFilter;

describe('<PublishedCell />', () => {
  it('render published cell', () => {
    render(<PublishedCell {...publishedFastFilter} />);

    const isPublished = screen.getByText('Да');

    expect(isPublished).toBeInTheDocument();
  });

  it('render unpublished cell', () => {
    render(<PublishedCell {...unpublishedFastFilter} />);

    const isPublished = screen.getByText('Нет');

    expect(isPublished).toBeInTheDocument();
  });
});
