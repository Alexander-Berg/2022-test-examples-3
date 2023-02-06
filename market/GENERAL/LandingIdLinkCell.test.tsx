import React from 'react';
import { render } from '@testing-library/react';

import { LandingIdLinkCell, LandingIdLinkCellProps } from './LandingIdLinkCell';

const props = {
  row: {
    id: 0,
    name: 'hello',
  },
};

describe('<LandingIdLinkCell />', () => {
  it('renders without errors', () => {
    render(<LandingIdLinkCell {...(props as LandingIdLinkCellProps)} />);
  });
});
