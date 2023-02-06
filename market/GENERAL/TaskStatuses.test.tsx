import { screen } from '@testing-library/react';

import { setupTestApp, TestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('TaskAudit Page:', () => {
  let testApp: TestApp;

  beforeEach(() => {
    testApp = setupTestApp({ route: ROUTES.TASK_AUDIT.path });
  });

  afterEach(() => {
    testApp.app.unmount();
  });

  it('displays correct title', () => {
    expect(screen.getByText(ROUTES.TASK_AUDIT.title.slice(1))).toBeInTheDocument();
  });
});
