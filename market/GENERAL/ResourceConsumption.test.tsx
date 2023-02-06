import { cleanup, render } from '@testing-library/react';
import React from 'react';

import { ResourceConsumption } from './ResourceConsumption';

describe('<ResourceConsumption />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(<ResourceConsumption />);
  });
});
