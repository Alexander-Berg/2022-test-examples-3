import React from 'react';
import { render } from '@testing-library/react';

import { TwoColumnLayout } from './TwoColumnLayout';

describe('<TwoColumnLayout />', () => {
  it('renders without errors', () => {
    render(<TwoColumnLayout />);
  });
});
