import React from 'react';
import { render } from '@testing-library/react';

import { Pager } from '.';

describe('<Pager />', () => {
  it('renders without errors', () => {
    render(<Pager total={1} itemCount={1} size="m" />);
  });
});
