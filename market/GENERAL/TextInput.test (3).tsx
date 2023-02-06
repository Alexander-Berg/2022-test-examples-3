import React from 'react';
import { render } from '@testing-library/react';

import { TextInput } from '.';

describe('<TextInput />', () => {
  it('renders without errors', () => {
    render(<TextInput onChange={() => 0} value="" />);
  });
});
