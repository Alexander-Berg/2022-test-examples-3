import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { Actions } from './Actions';

const data = TableUtils.getNewRowData(0);
describe('<Actions />', () => {
  it('renders without errors', () => {
    render(<Actions {...data} />);
  });
});
