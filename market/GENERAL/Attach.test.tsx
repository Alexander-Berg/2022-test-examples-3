import React from 'react';
import { render } from '@testing-library/react';

import { Attach } from '.';

describe('<Attach />', () => {
  it('renders without errors', () => {
    render(<Attach>some text</Attach>);
  });
});
