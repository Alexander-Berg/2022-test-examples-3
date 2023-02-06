import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Preloader from './Preloader';

describe('<Preloader />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<Preloader size="s" />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
