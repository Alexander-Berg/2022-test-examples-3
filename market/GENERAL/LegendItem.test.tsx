import React from 'react';
import { render } from '@testing-library/react';

import { LegendItem } from './LegendItem';

describe('<LegendItem/>', () => {
  it('render without errors', () => {
    render(<LegendItem color="#fff">Test</LegendItem>);
  });
});
