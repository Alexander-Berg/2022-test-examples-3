import * as React from 'react';
import * as ReactDOM from 'react-dom';

import { DevTaskContainer } from 'src/shared/toloka/DevTaskContainer';
import { task } from 'src/tasks/community-sku/task';
import { SampleData } from 'src/tasks/community-sku/sample-data';

it('renders without crashing', () => {
  const div = document.createElement('div');

  ReactDOM.render(<DevTaskContainer task={task} input={SampleData} />, div);

  expect(true).toBe(true);
});
