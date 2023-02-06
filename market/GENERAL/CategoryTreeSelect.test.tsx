import React from 'react';
import { screen } from '@testing-library/react';

import { renderWithProvider } from 'src/test/setupTestProvider';
import { CategoryTreeSelect } from '.';

describe('<CategoryTree />', () => {
  it('renders without errors', () => {
    renderWithProvider(<CategoryTreeSelect categories={[]} onSelect={jest.fn()} />);

    expect(screen.getByRole('button')).toBeInTheDocument();
  });
});
