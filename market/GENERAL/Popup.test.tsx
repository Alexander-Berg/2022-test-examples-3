import React from 'react';
import { render } from '@testing-library/react';

import { Popup } from '.';

describe('<Popup />', () => {
  it('renders without errors', () => {
    render(<Popup />);
  });
});
