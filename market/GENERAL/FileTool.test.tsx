import { screen } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';

describe('FileTool Page:', () => {
  it('displays correct title', () => {
    setupTestApp({ route: ROUTES.FILE_TOOL.path });
    expect(screen.getByText(ROUTES.FILE_TOOL.title.slice(1))).toBeInTheDocument();
  });
});
