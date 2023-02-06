import React from 'react';
import { render } from '@testing-library/react';

import { ValuesSelect } from './ValuesSelect';

describe('<ValuesSelect />', () => {
  it('renders empty data without errors', () => {
    render(<ValuesSelect values={[]} selectedValues={[]} onChange={() => 1} />);
  });

  it('renders data without errors', () => {
    render(<ValuesSelect values={[{ name: 'undefined', value: -1 }]} selectedValues={[-2]} onChange={() => 1} />);
  });
});
