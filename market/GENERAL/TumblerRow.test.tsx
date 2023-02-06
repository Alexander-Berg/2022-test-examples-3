import React from 'react';
import { render } from '@testing-library/react';

import { TumblerRow } from 'src/components/TumblerTable/components/TumblerRow/TumblerRow';

describe('<TumblerTable />', () => {
  it('renders without errors', () => {
    render(<TumblerRow id="test" options={[]} checkedIds={[]} onChange={jest.fn()} />);
  });
});
