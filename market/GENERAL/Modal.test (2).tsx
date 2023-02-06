import React from 'react';
import { render } from '@testing-library/react';

import { Modal } from '.';

describe('<Modal />', () => {
  it('renders without errors', () => {
    render(<Modal />);
  });
});
