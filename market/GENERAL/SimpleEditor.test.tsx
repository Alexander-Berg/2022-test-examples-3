import React from 'react';
import { render } from '@testing-library/react';

import { SimpleEditor } from './SimpleEditor';

describe('<SimpleEditor />', () => {
  it('renders without errors', () => {
    render(<SimpleEditor onChange={jest.fn()} />);
  });
});
