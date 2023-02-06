import * as React from 'react';
import * as ReactDOM from 'react-dom';
import CartsTable from './CartsTable';

describe('<CartsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<CartsTable carts={[]} actionsRenderer={jest.fn()} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
