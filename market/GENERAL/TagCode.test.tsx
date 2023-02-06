import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { TagCode } from './TagCode';

const data = TableUtils.getNewRowData(0);
describe('<TagCode />', () => {
  it('renders without errors', () => {
    render(<TagCode {...data} />);
  });
});
