import React from 'react';
import { render } from '@testing-library/react';

import { MessageBox } from '.';

describe('<MessageBox />', () => {
  it('renders without errors', () => {
    render(<MessageBox />);
  });
});
