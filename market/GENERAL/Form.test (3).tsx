import React from 'react';
import { render } from '@testing-library/react';

import { Form } from './Form';

describe('<Form />', () => {
  it('renders without errors', () => {
    render(<Form model={{} as any} />);
  });
});
