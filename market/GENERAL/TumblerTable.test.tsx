import React from 'react';
import { render } from '@testing-library/react';

import { TumblerTable } from './TumblerTable';

describe('<TumblerTable />', () => {
  it('renders without errors', () => {
    render(<TumblerTable options={[]} rows={[]} onChange={jest.fn()} />);
  });
});
