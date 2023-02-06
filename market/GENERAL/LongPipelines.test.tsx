import { act } from '@testing-library/react';

import { setupTestApp } from 'src/test/setupApp';
import { LONG_PIPELINES } from '../common/constants';

describe('LongPipelines', () => {
  test('load', () => {
    const { api } = setupTestApp(`/reports${LONG_PIPELINES.url}`);

    act(() => {
      expect(api.goodContentController.listLongPipelines.activeRequests().length).toBe(1);
      api.goodContentController.listLongPipelines.next().resolve({ count: 0, rows: [] } as any);
      expect(api.goodContentController.listLongPipelines.activeRequests().length).toBe(0);
    });
  });
});
