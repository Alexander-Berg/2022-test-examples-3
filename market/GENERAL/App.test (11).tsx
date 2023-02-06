import * as React from 'react';
import * as ReactDOM from 'react-dom';

import ExampleApp from './ExampleApp';

it('renders without crashing', () => {
  const div = document.createElement('div');

  const input = {
    content: 'Hello World!',
  };

  const onSubmit = jest.fn();

  const root = <ExampleApp input={input} onSubmit={onSubmit} />;

  ReactDOM.render(root, div);

  expect(true).toBe(true);
});
