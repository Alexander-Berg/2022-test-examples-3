import React from 'react';
import { render } from '@testing-library/react';

import { Tabs } from './Tabs';

describe('<Tabs />', () => {
  it('renders without errors', () => {
    render(<Tabs />);
  });
});
