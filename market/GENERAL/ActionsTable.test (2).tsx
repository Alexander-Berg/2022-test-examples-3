import React from 'react';
import ReactDOM from 'react-dom';

import { ActionsTable } from './ActionsTable';

describe('<CartsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<ActionsTable actions={[]} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
