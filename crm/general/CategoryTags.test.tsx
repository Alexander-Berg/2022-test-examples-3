import React from 'react';
import { render, screen } from '@testing-library/react';
import { CategoryTags } from './CategoryTags';

describe('CategoryTags', () => {
  it('renders props.tags', () => {
    render(<CategoryTags tags={['123', '321', '456']} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });
});
