import * as React from 'react';
import * as ReactDOM from 'react-dom';
import MovementsTable from './MovementsTable';

describe('<MovementsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<MovementsTable movements={[]} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
