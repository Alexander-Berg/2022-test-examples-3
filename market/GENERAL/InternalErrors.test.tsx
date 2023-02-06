import { fireEvent, waitFor } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupApp';
import { INTERNAL_ERRORS } from '../common/constants';

describe('LongPipelines', () => {
  test('load', async () => {
    const { app, api } = setupTestApp(`/reports${INTERNAL_ERRORS.url}`);

    app.getAllByText(new RegExp(INTERNAL_ERRORS.name, 'i'));
    fireEvent.click(app.getByText('Обновить'));

    await waitFor(() => {
      expect(api.goodContentController.listInternalErrors.activeRequests().length).toBe(1);
      api.goodContentController.listInternalErrors.next().resolve({ count: 0, rows: [] } as any);
      expect(api.goodContentController.listInternalErrors.activeRequests().length).toBe(0);
    });
  });
});
