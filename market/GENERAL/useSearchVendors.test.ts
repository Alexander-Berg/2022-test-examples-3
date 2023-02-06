/* eslint-disable no-console */
import { renderHook } from '@testing-library/react-hooks';
import { VendorsRequest } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { act } from 'react-dom/test-utils';
import { createStore } from 'redux';
import { BehaviorSubject } from 'rxjs';

import { getLogsWrapper } from 'src/tasks/common-logs/test/utils/getLogsWrapper';
import { useSearchVendors } from './useSearchVendors';

const getWrapper = () => {
  const requestSubject = new BehaviorSubject<VendorsRequest | undefined>(undefined);

  const wrapper = getLogsWrapper({
    searchVendors: request => {
      requestSubject.next(request);

      return Promise.resolve({ vendor: [] });
    },
  });

  return { requestSubject, wrapper };
};

describe('useSearchVendors', () => {
  it('works with empty query', async () => {
    const { requestSubject, wrapper } = getWrapper();

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123 }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('');
    });

    expect(vendors).toEqual([]);
    expect(requestSubject.value).toEqual({
      category_id: 123,
      limit: 10000,
      only_local: true,
      query: undefined,
    });
  });
  it('pass local flag', async () => {
    const { requestSubject, wrapper } = getWrapper();

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123, onlyLocal: false }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('');
    });

    expect(vendors).toEqual([]);
    expect(requestSubject.value).toEqual({
      category_id: 123,
      limit: 10000,
      only_local: false,
      query: undefined,
    });
  });
  it('pass query', async () => {
    const { requestSubject, wrapper } = getWrapper();

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123, query: 'test' }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('');
    });

    expect(vendors).toEqual([]);
    expect(requestSubject.value).toEqual({
      category_id: 123,
      limit: 10000,
      only_local: true,
      query: 'test',
    });
  });
  it('pass second query', async () => {
    const { requestSubject, wrapper } = getWrapper();

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123, query: 'test' }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('test2');
    });

    expect(vendors).toEqual([]);
    expect(requestSubject.value).toEqual({
      category_id: 123,
      limit: 10000,
      only_local: true,
      query: 'test2',
    });
  });
  it('works with empty response', async () => {
    const wrapper = getLogsWrapper({
      searchVendors: () => Promise.resolve({}),
    });

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123 }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('');
    });

    expect(vendors).toEqual([]);
  });
  it('filters response', async () => {
    const wrapper = getLogsWrapper({
      searchVendors: () =>
        Promise.resolve({ vendor: [undefined as any, { vendor_id: 1 }, { vendor_id: 2, name: 'test' }] }),
    });

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123 }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('');
    });

    expect(vendors).toEqual([
      {
        name: 'test',
        vendor_id: 2,
      },
    ]);
  });
  it('sorts response', async () => {
    const wrapper = getLogsWrapper({
      searchVendors: () =>
        Promise.resolve({
          vendor: [
            { vendor_id: 1, name: 'test2' },
            { vendor_id: 2, name: 'test1' },
            { vendor_id: 3, name: 'test3' },
          ],
        }),
    });

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123 }), { wrapper });

    let vendors;

    await act(async () => {
      vendors = await current('');
    });

    expect(vendors).toEqual([
      { vendor_id: 2, name: 'test1' },
      { vendor_id: 1, name: 'test2' },
      { vendor_id: 3, name: 'test3' },
    ]);
  });
  it('handle error response', async () => {
    const consoleOutput: any[] = [];
    const oldWarn = console.warn;
    console.warn = output => consoleOutput.push(output);

    const actions: any[] = [];
    const store = createStore((_, action) => {
      actions.push(action);
    });

    const testError = new Error('test');

    const wrapper = getLogsWrapper(
      {
        searchVendors: () => Promise.reject(testError),
      },
      store
    );

    const {
      result: { current },
    } = renderHook(() => useSearchVendors({ categoryId: 123 }), { wrapper });

    await act(async () => {
      await current('');
    });

    expect(consoleOutput).toHaveLength(1);
    expect(consoleOutput[0]).toEqual(testError);

    console.warn = oldWarn;
  });
});
