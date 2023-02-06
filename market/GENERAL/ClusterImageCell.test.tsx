import React from 'react';
import { render, screen } from '@testing-library/react';

import { ClusterImageCell, ClusterImageCellProps } from './ClusterImageCell';

const cellProps: ClusterImageCellProps = ({ row: { picture_url: '' } } as unknown) as ClusterImageCellProps;

describe('<ClusterImageCell/>', () => {
  it('renders without errors', () => {
    render(<ClusterImageCell {...cellProps} />);
  });

  it('contains image', () => {
    render(<ClusterImageCell {...cellProps} />);

    expect(screen.getByRole('img')).toBeInTheDocument();
  });
});
