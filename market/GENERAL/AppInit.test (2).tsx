import React from 'react';
import ReactDOM from 'react-dom';
import { AppInit } from './AppInit';

it('renders without crashing', () => {
  const div = document.createElement('div');
  ReactDOM.render(<AppInit />, div);
  ReactDOM.unmountComponentAtNode(div);
});
