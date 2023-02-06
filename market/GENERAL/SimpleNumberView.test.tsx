import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { SimpleNumberView } from './SimpleNumberView';

const data = TableUtils.getNewRowData(0);
describe('<SimpleNumberView />', () => {
  it('renders without errors', () => {
    render(<SimpleNumberView {...data} />);
  });
});
