/* eslint-disable no-console */
import { renderHook } from '@testing-library/react-hooks';
import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { act } from 'react-dom/test-utils';
import { createStore } from 'redux';

import { getLogsWrapper } from 'src/tasks/common-logs/test/utils/getLogsWrapper';
import { useSearchModels } from './useSearchModels';

describe('useSearchModels', () => {
  it('should pass request', async () => {
    let lastRequest;
    const request = { category_id: 123, vendor_id: 432, limit: 200, query: 'test' };

    const wrapper = getLogsWrapper({
      searchModels: r => {
        lastRequest = r;

        return Promise.resolve({ model: [] });
      },
    });

    const {
      result: { current },
    } = renderHook(() => useSearchModels(), { wrapper });

    act(() => {
      current(request);
    });

    expect(lastRequest).toEqual({
      ...request,
      model_type: 'GURU',
    });
  });

  it('returns models', async () => {
    const models = [{ id: 123 }];

    const wrapper = getLogsWrapper({
      searchModels: () => {
        return Promise.resolve({ result: { status: OperationStatus.SUCCESS }, model: models });
      },
    });

    const {
      result: { current },
    } = renderHook(() => useSearchModels(), { wrapper });

    let models2;

    await act(async () => {
      models2 = await current({});
    });

    expect(models2).toEqual(models);
  });
  /*
   * In case when AM requests falls with error, hook should:
   * 1) return Promise.resolve([])
   * 2) pass error to console.warn
   * 3) dispatch action to show error toast
   */
  it('handle error response', async () => {
    const consoleOutput: any[] = [];
    // eslint-disable-next-line no-console
    const oldWarn = console.warn;
    // eslint-disable-next-line no-console
    console.warn = output => consoleOutput.push(output);

    const actions: any[] = [];
    const store = createStore((_, action) => {
      actions.push(action);
    });

    const testError = new Error('test');

    const wrapper = getLogsWrapper(
      {
        searchModels: () => Promise.reject(testError),
      },
      store
    );

    const {
      result: { current },
    } = renderHook(() => useSearchModels(), { wrapper });

    await act(async () => {
      await current({ category_id: 123, vendor_id: 432, query: 'test' });
    });

    // expect(actions).toHaveLength(2);
    // expect(actions[1].payload.theme).toEqual('error');
    expect(consoleOutput).toHaveLength(1);
    expect(consoleOutput[0]).toEqual(testError);

    console.warn = oldWarn;
  });
  /*
   * In case when AM response with incorrect data:
   * 1) return Promise.resolve([])
   * 2) pass error to console.warn
   * 3) dispatch action to show error toast
   */
  it('handle incorrect response', async () => {
    const consoleOutput: any[] = [];
    const oldWarn = console.warn;
    console.warn = output => consoleOutput.push(output);

    const actions: any[] = [];
    const store = createStore((_, action) => {
      actions.push(action);
    });

    const wrapper = getLogsWrapper(
      {
        searchModels: () => Promise.resolve({ reqId: 123 } as any),
      },
      store
    );

    const {
      result: { current },
    } = renderHook(() => useSearchModels(), { wrapper });

    await act(async () => {
      await current({});
    });

    expect(consoleOutput).toHaveLength(1);
    expect(consoleOutput[0]).toEqual('Wrong response: {"reqId":123}');

    console.warn = oldWarn;
  });
});
