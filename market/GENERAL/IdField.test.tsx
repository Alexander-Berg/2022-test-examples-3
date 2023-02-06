import React from 'react';
import { render } from '@testing-library/react';

import { IdField } from './IdField';

describe('<IdField />', () => {
  it('renders without errors', () => {
    render(<IdField id={0} />);
  });
});
