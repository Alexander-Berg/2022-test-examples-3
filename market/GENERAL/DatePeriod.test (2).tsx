import React from 'react';
import { render } from '@testing-library/react';

import { DatePeriod } from '.';

describe('<DatePeriod />', () => {
  it('renders without errors', () => {
    render(<DatePeriod onStartChange={() => 0} onEndChange={() => 0} />);
  });
});
