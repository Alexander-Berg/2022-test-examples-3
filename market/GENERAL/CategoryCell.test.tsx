import React from 'react';
import { screen } from '@testing-library/react';

import { renderTableCell } from 'src/test/setupTestProvider';
import { CategoryCell } from '.';

describe('<CategoryCell />', () => {
  it('should be shown poolName', () => {
    renderTableCell(<CategoryCell poolName="Test pool" guruCategoryId={100} guruCategoryName="Some category" />);

    expect(screen.getByText(/Test pool/i)).toBeInTheDocument();
  });

  it('should be shown categoryName', () => {
    renderTableCell(<CategoryCell guruCategoryId={100} guruCategoryName="Some category" />);

    expect(screen.getByText(/Some category/i)).toBeInTheDocument();
  });

  it('should be shown default value', () => {
    renderTableCell(<CategoryCell />);

    expect(screen.getByText(/–/)).toBeInTheDocument();
  });
});
