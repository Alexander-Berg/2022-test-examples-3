import { getByText, cleanup } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupApp';

test('Simple render', () => {
  const { app } = setupTestApp('');
  expect(getByText(app.container, 'Отчеты')).toBeInTheDocument();
  cleanup();
});
