import ActionsTable from 'src/pages/Audit/components/ActionsTable/ActionsTable';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

describe('<CartsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<ActionsTable actions={[]} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
