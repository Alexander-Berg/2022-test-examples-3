import * as React from 'react';
import * as ReactDOM from 'react-dom';
import RowItem from './Row-Item';

describe('<RowItem />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<RowItem />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
