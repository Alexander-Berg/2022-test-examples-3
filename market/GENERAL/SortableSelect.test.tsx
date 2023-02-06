import React from 'react';
import { render, screen } from '@testing-library/react';

import { SortableSelect } from './SortableSelect';

describe('<SortableSelect />', () => {
  it('renders without errors', () => {
    const options = [
      {
        value: 1,
        label: 'Testiya Testikovna',
      },
      {
        value: 3,
        label: 'Testische Testiman',
      },
      {
        value: 9876543456789876543,
        label: 'Testik Testovich Testinberg',
      },
    ];

    render(
      <SortableSelect options={options} selectedValues={[options[0].value, options[2].value]} onChange={() => 1} />
    );
    expect(screen.getByText(options[0].label)).not.toBeNull();
    expect(screen.queryByText(options[1].label)).toBeNull();
    expect(screen.getByText(options[2].label)).not.toBeNull();
  });
});
