import { BillingStatsTable } from 'src/pages/Admin/components/BillingStatsTable/BillingStatsTable';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

describe('<BillingStatsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<BillingStatsTable stats={[]} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
