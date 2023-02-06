import { screen } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('CwVerdicts Page:', () => {
  it('displays correct title', () => {
    setupTestApp({ route: ROUTES.CW_VERDICTS.path });
    expect(screen.getByText(ROUTES.CW_VERDICTS.title.slice(1))).toBeInTheDocument();
  });
});
