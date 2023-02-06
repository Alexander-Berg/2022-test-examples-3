import React from 'react';
import { render } from '@testing-library/react';

import { BooleanName } from './BooleanName';
import TableUtils from '../../Table.utils';

describe('<BooleanName />', () => {
  it('renders without errors', () => {
    const data = TableUtils.getNewRowData(0);
    render(<BooleanName {...data} />);
  });
});
