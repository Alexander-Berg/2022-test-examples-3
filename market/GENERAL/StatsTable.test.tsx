import React from 'react';
import ReactDOM from 'react-dom';

import { StatsTable } from './StatsTable';

describe('<StatsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<StatsTable stats={[]} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
