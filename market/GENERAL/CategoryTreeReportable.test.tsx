import React from 'react';
import { screen } from '@testing-library/react';

import { renderWithProvider } from 'src/test/setupTestProvider';
import { CategoryTreeReportable } from '.';

describe('<CategoryTreeReportable />', () => {
  it('renders without errors', () => {
    renderWithProvider(<CategoryTreeReportable categories={[]} onSelect={jest.fn()} />);

    expect(screen.getByRole('tree')).toBeInTheDocument();
  });
});
