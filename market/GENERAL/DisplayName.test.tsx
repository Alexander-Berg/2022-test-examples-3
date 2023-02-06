import React from 'react';
import { render } from '@testing-library/react';

import { DisplayName } from './DisplayName';
import TableUtils from '../../Table.utils';

const data = TableUtils.getNewRowData(0);
describe('<DisplayName />', () => {
  it('renders without errors', () => {
    render(<DisplayName {...data} />);
  });
});
