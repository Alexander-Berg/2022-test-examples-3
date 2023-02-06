import { cleanup, render } from '@testing-library/react';
import React from 'react';

import { TextLog } from './TextLog';

describe('<TextLog />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders without errors', () => {
    render(<TextLog id={5} />);
  });
});
