import { screen } from '@testing-library/react';

import { setupTestApp, TestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('Statistics Page:', () => {
  let testApp: TestApp;

  beforeEach(() => {
    testApp = setupTestApp({ route: ROUTES.STATISTIC.path });
  });

  afterEach(() => {
    testApp.app.unmount();
  });

  it('displays correct title', () => {
    expect(screen.getByText(ROUTES.STATISTIC.title.slice(1))).toBeInTheDocument();
  });
});
