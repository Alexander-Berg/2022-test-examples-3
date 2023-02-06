import React from 'react';
import { render } from '@testing-library/react';

import { Accordion } from './Accordion';

describe('<Accordion />', () => {
  it('renders without errors', () => {
    render(<Accordion title="Expand Me" />);
  });
});
