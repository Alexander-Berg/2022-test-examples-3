import React from 'react';
import { render } from '@testing-library/react';

import { ButtonWithLoader } from '.';

describe('<ButtonWithLoader />', () => {
  it('renders without errors', () => {
    render(<ButtonWithLoader text="some text" onClick={() => 0} />);
  });

  it('renders loading without errors', () => {
    render(<ButtonWithLoader text="some text" onClick={() => 0} loading loadingText="Loading..." />);
  });
});
