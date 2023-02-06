import { screen } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('TaskAudit Page:', () => {
  it('displays correct title', () => {
    setupTestApp({ route: ROUTES.TASK_AUDIT.path });
    expect(screen.getByText(ROUTES.TASK_AUDIT.title.slice(1))).toBeInTheDocument();
  });
});
