import React from 'react';
import { render } from '@testing-library/react';

import { LandingNameLinkCell, LandingNameLinkCellProps } from './LandingNameLinkCell';

const props = {
  row: {
    id: 0,
    name: 'hello',
  },
};

describe('<LandingNameLinkCell />', () => {
  it('renders without errors', () => {
    render(<LandingNameLinkCell {...(props as LandingNameLinkCellProps)} onClickOnLanding={() => null} />);
  });
});
