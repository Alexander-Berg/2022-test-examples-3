import React from 'react';
import { render } from '@testing-library/react';

import { DateRangePicker } from './DateRangePicker';

describe('<DateRangePicker />', () => {
  it('renders without errors', () => {
    render(<DateRangePicker onChange={() => undefined} startDate={new Date()} endDate={new Date()} delimiter=" - " />);
  });
});
