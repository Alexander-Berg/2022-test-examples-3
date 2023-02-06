import * as React from 'react';
import * as ReactDOM from 'react-dom';
import BarInfo from './BarInfo';

describe('<BarInfo />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<BarInfo />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
