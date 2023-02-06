import React from 'react';
import { render } from '@testing-library/react';

import { DatePicker } from '.';

describe('<DatePicker />', () => {
  it('renders without errors', () => {
    render(<DatePicker onChange={() => 0} />);
  });
});
