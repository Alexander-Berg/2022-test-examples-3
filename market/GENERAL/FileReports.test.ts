import { fireEvent, waitFor } from '@testing-library/react';

import { FileProcessStatus, GcFileData, PipelineStatus, PipelineType } from 'src/rest/definitions';
import { setupTestApp } from 'src/test/setupApp';
import { FILE_REPORT } from '../common/constants';

const fileReports: GcFileData[] = [
  {
    requestId: 6789,
    processId: 6602,
    sourceId: 94234,
    sourceName: 'Проверка 100%',
    fileUrl:
      'https://market-ir-prod.s3.mds.yandex.net/robot/models/olTl8eqNoctyouvYdmWw8NXUzVQGCbNaVKXLnOd5kQusJR5rx0aKoiWhOPK3OH31.xlsx',
    startTs: 2,
    finishTs: 2,
    categories: [13199892],
    partnerShopId: 1,
    status: FileProcessStatus.INVALID,
    pipelineStatus: PipelineStatus.FINISHED,
    pipelineType: PipelineType.GOOD_CONTENT_SINGLE_XLS,
  },
];

describe('LongPipelines', () => {
  test('load', async () => {
    const { app, api } = setupTestApp(`/reports${FILE_REPORT.url}`);

    app.getAllByText(new RegExp(FILE_REPORT.name, 'i'));
    fireEvent.click(app.getByText('Обновить'));

    await waitFor(() => {
      expect(api.goodContentController.listGcFileData.activeRequests().length).toBe(1);
      api.goodContentController.listGcFileData.next().resolve({ count: 0, rows: [fileReports] } as any);
      expect(api.goodContentController.listGcFileData.activeRequests().length).toBe(0);
    });
  });
});
