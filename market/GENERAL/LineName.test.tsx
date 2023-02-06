import React from 'react';
import { render } from '@testing-library/react';

import TableUtils from '../../Table.utils';
import { LineName } from './LineName';

const data = TableUtils.getNewRowData(0);
describe('<LineName />', () => {
  it('renders without errors', () => {
    render(<LineName {...data} />);
  });
});
