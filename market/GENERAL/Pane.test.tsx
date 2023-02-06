import { render } from '@testing-library/react';
import React from 'react';

import { Pane } from './Pane';

describe('Pane::', () => {
  it('renders without errors', () => {
    render(<Pane>test</Pane>);
  });
});
