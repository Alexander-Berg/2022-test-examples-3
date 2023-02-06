import { fireEvent, waitFor } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupApp';
import { DCP_REPORTS } from '../common/constants';

describe('DCPReports', () => {
  test('load', async () => {
    const { app, api } = setupTestApp(`/reports${DCP_REPORTS.url}`);

    app.getAllByText(new RegExp(DCP_REPORTS.name, 'i'));
    fireEvent.click(app.getByText('Обновить'));

    await waitFor(() => {
      expect(api.goodContentController.listDataCampOffers.activeRequests().length).toBe(1);
      api.goodContentController.listDataCampOffers.next().resolve({ count: 0, rows: [] } as any);
      expect(api.goodContentController.listDataCampOffers.activeRequests().length).toBe(0);
    });
  });
});
