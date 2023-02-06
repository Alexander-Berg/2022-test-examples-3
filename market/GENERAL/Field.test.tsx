import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Field from './Field';

describe('<Field />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<Field />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
