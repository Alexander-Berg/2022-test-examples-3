import React from 'react';
import { render } from '@testing-library/react';

import { RecipesNameLinkCell, RecipesNameLinkCellProps } from './RecipesNameLinkCell';

const props = {
  row: {
    id: 0,
    node: {},
  },
};

describe('<RecipesNameLinkCell />', () => {
  it('renders without errors', () => {
    render(<RecipesNameLinkCell {...(props as RecipesNameLinkCellProps)} />);
  });
});
