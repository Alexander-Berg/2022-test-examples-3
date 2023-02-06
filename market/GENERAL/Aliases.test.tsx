import React from 'react';
import { render } from '@testing-library/react';

import { Aliases } from './Aliases';
import TableUtils from '../../Table.utils';

const data = TableUtils.getNewRowData(0);
describe('<Aliases />', () => {
  it('renders empty array without errors', () => {
    render(<Aliases {...data} />);
  });

  it('renders without errors', () => {
    render(<Aliases {...data} />);
  });
});
