import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Breadcrumbs from './Breadcrumbs';

describe('<Breadcrumbs />', () => {
  it('renders without crashing', () => {
    const div = document.createElement('div');

    ReactDOM.render(<Breadcrumbs />, div);
    ReactDOM.unmountComponentAtNode(div);
  });
});
