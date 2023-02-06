import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { ManufactureVendorNames } from './ManufactyrerName';

const data = TableUtils.getNewRowData(0);
describe('<ManufactureVendorNames />', () => {
  it('renders without errors', () => {
    render(<ManufactureVendorNames {...data} />);
  });
});
