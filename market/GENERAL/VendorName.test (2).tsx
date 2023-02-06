import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { VendorName } from './VendorName';

const data = TableUtils.getNewRowData(0);
describe('<VendorName />', () => {
  it('renders without errors', () => {
    render(<VendorName {...data} />);
  });
});
