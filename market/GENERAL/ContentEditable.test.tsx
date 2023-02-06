import React from 'react';
import { render } from '@testing-library/react';

import { ContentEditable } from '.';

describe('<ContentEditable />', () => {
  it('renders without errors', () => {
    render(<ContentEditable onChange={jest.fn()} />);
  });
});
