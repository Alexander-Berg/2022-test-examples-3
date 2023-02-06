import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Row from './Row';

describe('<Row />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<Row />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
