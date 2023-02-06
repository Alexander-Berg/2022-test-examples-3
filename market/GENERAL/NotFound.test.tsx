import { screen } from '@testing-library/react';

import { setupTestApp, TestApp } from 'src/test/setupTestApp';

describe('NotFound::', () => {
  let testApp: TestApp;

  beforeEach(() => {
    testApp = setupTestApp({ route: '/ui/foo' });
  });

  afterEach(() => {
    testApp.app.unmount();
  });

  it('should render the component', () => {
    expect(screen.getByText('акой страницы не существует')).toBeInTheDocument();
  });
});
