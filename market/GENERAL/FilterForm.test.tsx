import React from 'react';
import { render } from '@testing-library/react';

import { FilterForm } from '.';

describe('<FilterForm />', () => {
  it('renders without errors', () => {
    const config = {
      onChange: () => 0,
    };
    render(<FilterForm config={config} data={[]} />);
  });
});
