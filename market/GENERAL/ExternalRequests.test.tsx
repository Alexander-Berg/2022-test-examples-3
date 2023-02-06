import { fireEvent, waitFor } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupApp';
import { EXTERNAL_REQUESTS } from '../common/constants';

describe('LongPipelines', () => {
  test('load', async () => {
    const { app, api } = setupTestApp(`/reports${EXTERNAL_REQUESTS.url}`);
    app.getAllByText(new RegExp(EXTERNAL_REQUESTS.name, 'i'));
    fireEvent.click(app.getByText('Обновить'));

    await waitFor(() => {
      expect(api.goodContentController.listExternalRequests.activeRequests().length).toBe(1);
      api.goodContentController.listExternalRequests.next().resolve({ count: 0, rows: [] } as any);
      expect(api.goodContentController.listExternalRequests.activeRequests().length).toBe(0);
    });
  });
});
