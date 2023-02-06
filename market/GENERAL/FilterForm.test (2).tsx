import { screen } from '@testing-library/react';

import { ROUTES } from 'src/constants';
import { setupTestApp, TestApp } from 'src/test/setupTestApp';

describe('FilterForm', () => {
  let testApp: TestApp;

  beforeEach(() => {
    testApp = setupTestApp({ route: ROUTES.TASK_STATUSES.path });
  });

  afterEach(() => {
    testApp.app.unmount();
  });

  it('should render the task statuses filter form', () => {
    expect(screen.getByText(ROUTES.TASK_STATUSES.title.slice(1))).toBeInTheDocument();
  });
});
