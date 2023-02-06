import React from 'react';
import { render } from '@testing-library/react';

import { TovarTreeNodeWithPathDto } from 'src/java/definitions';
import { CategoryItem } from './CategoryItem';

const linkedNodes = {
  hid: 1,
  name: 'tree',
  path: [
    {
      hid: 1,
      name: 'tree',
    },
  ],
} as TovarTreeNodeWithPathDto;

describe('<CategoryItem />', () => {
  it('renders without errors', () => {
    render(<CategoryItem linkedNodes={linkedNodes} />);
  });
});
