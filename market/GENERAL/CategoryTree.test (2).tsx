import React from 'react';
import { screen } from '@testing-library/react';

import { renderWithProvider } from 'src/test/setupTestProvider';
import { CategoryTree } from '.';

describe('<CategoryTree />', () => {
  it('renders without errors', () => {
    renderWithProvider(<CategoryTree categories={[]} onSelect={jest.fn()} />);

    expect(screen.getByRole('tree')).toBeInTheDocument();
  });
});
