import * as React from 'react';
import * as ReactDOM from 'react-dom';
import GoodsTable from './GoodsTable';

describe('<GoodsTable />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<GoodsTable goods={[]} />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
