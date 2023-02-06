import { screen } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('AuditOld Page:', () => {
  it('displays correct title', () => {
    setupTestApp({ route: ROUTES.AUDIT_OLD.path });
    expect(screen.getByText(ROUTES.AUDIT_OLD.title.slice(1))).toBeInTheDocument();
  });
});
