import React from 'react';
import { render, screen } from '@testing-library/react';

import { ClusterLinkCell, ClusterLinkCellProps } from './ClusterLinkCell';

const TEST_CLUSTER_ID = 111;
const TEST_CATEGORY_ID = 222;

const cellProps: ClusterLinkCellProps = ({
  row: { id: TEST_CLUSTER_ID },
  context: { categoryId: TEST_CATEGORY_ID },
} as unknown) as ClusterLinkCellProps;

describe('<ClusterLinkCell/>', () => {
  it('renders without errors', () => {
    render(<ClusterLinkCell {...cellProps} />);
  });

  it('have url link only with clusterId', async () => {
    render(<ClusterLinkCell {...cellProps} />);

    const items = await screen.findAllByRole('link');
    expect(items).toHaveLength(2);
    const [urlLink] = items;
    expect(urlLink).toHaveAttribute('href', expect.stringContaining(`${TEST_CLUSTER_ID}`));
    expect(urlLink).toHaveAttribute('href', expect.not.stringContaining(`${TEST_CATEGORY_ID}`));
  });

  it('have icon link with both with clusterId/categoryId', async () => {
    render(<ClusterLinkCell {...cellProps} />);

    const items = await screen.findAllByRole('link');
    expect(items).toHaveLength(2);
    const [, iconLink] = items;
    expect(iconLink).toHaveAttribute('href', expect.stringContaining(`${TEST_CLUSTER_ID}`));
    expect(iconLink).toHaveAttribute('href', expect.stringContaining(`${TEST_CATEGORY_ID}`));
  });
});
