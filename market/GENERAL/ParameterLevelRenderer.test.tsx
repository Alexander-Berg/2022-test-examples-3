import React from 'react';
import { render } from '@testing-library/react';

import { ParameterLevelRenderer } from './ParameterLevelRenderer';

describe('<ParameterLevelRenderer/>', () => {
  it('renders without errors', () => {
    render(<ParameterLevelRenderer value="string" />);
  });
});
