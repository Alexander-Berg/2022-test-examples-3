import { renderHook } from '@testing-library/react-hooks';
import { Vendor } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { act } from 'react-dom/test-utils';

import { useSearchVendors } from 'src/shared/hooks/useSearchVendors';
import { useVendorOptionsSearch } from 'src/shared/hooks/useVendorOptionsSearch';
import { getWrapper } from 'src/tasks/common-logs/test/utils/getLogsWrapper';

jest.mock('./useSearchVendors');

const useSearchVendorsMock = useSearchVendors as jest.Mock<(query?: string) => Promise<Vendor[]>>;

describe('useVendorOptionsSearch', () => {
  useSearchVendorsMock.mockReturnValue(() => Promise.resolve([]));

  it('return default options', () => {
    const wrapper = getWrapper({
      initialState: { vendors: { defaultVendors: [{ vendor_id: 1, name: 'test' }] } },
    });

    const {
      result: { current },
    } = renderHook(() => useVendorOptionsSearch(123), { wrapper });

    const [_, defaultOptions] = current;

    expect(defaultOptions).toEqual([
      {
        label: 'test',
        vendor: {
          name: 'test',
          vendor_id: 1,
        },
      },
    ]);
  });
  it('return default local options', () => {
    const wrapper = getWrapper({
      initialState: { vendors: { defaultLocalVendors: [{ vendor_id: 2, name: 'test2' }] } },
    });

    const {
      result: { current },
    } = renderHook(() => useVendorOptionsSearch(123, true), { wrapper });

    const [_, defaultOptions] = current;

    expect(defaultOptions).toEqual([
      {
        label: 'test2',
        vendor: {
          name: 'test2',
          vendor_id: 2,
        },
      },
    ]);
  });
  it('search', async () => {
    useSearchVendorsMock.mockReturnValue(() => Promise.resolve([{ vendor_id: 1, name: 'test' }]));

    const wrapper = getWrapper({
      initialState: { vendors: {} },
    });

    const {
      result: { current },
    } = renderHook(() => useVendorOptionsSearch(123, true), { wrapper });

    const [subject] = current;

    let options;

    await act(async () => {
      await new Promise<void>(resolve => {
        // eslint-disable-next-line no-unused-expressions
        subject.current?.next({
          searchText: 'test',
          callback: opts => {
            options = opts;

            resolve();
          },
        });
      });
    });

    expect(options).toEqual([
      {
        label: 'test',
        vendor: {
          name: 'test',
          vendor_id: 1,
        },
      },
    ]);
  });
});
