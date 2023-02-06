import { screen } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('Audit Page:', () => {
  it('displays correct title', () => {
    setupTestApp({ route: ROUTES.AUDIT.path });
    expect(screen.getByText(ROUTES.AUDIT.title.slice(1)).tagName).toEqual('H1');
  });
});
