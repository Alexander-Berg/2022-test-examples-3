import { renderHook, act } from '@testing-library/react-hooks';
import { SearchModelsRequest, SuggestedModel } from '@yandex-market/market-proto-dts/Market/AliasMaker';

import { useSearchModels } from '../../../hooks/useSearchModels';
import { useLoadModels } from './useLoadModels';

jest.mock('../../../hooks/useSearchModels');
jest.mock('src/shared/services/instances', () => {
  return {
    errorLogger: {
      addErrorSelectors: jest.fn(),
      describeOnError: jest.fn(),
      getErrorAdditional: jest.fn(),
      logException: jest.fn(),
      setErrorAdditional: jest.fn(),
    },
  };
});

const useSearchModelsMock = useSearchModels as jest.Mock<(request: SearchModelsRequest) => Promise<SuggestedModel[]>>;

describe('useLoadModels', () => {
  it('works without vendor', () => {
    useSearchModelsMock.mockReturnValue(jest.fn());
    const {
      result: {
        current: { models, isLoading },
      },
    } = renderHook(() => useLoadModels(123, '', 0));

    expect(models).toEqual([]);
    expect(isLoading).toBeFalse();
  });
  it('works with vendor', async () => {
    let resolve: any;
    let vendorId: number | undefined;
    const testVendorId = 234;

    const models = [{ id: 123 }];

    useSearchModelsMock.mockReturnValue(({ vendor_id }) => {
      vendorId = vendor_id;

      return new Promise(resolve1 => {
        resolve = resolve1;
      });
    });
    const { result } = renderHook(() => useLoadModels(123, '', testVendorId));

    expect(result.current.models).toEqual([]);
    expect(result.current.isLoading).toBeTrue();
    expect(vendorId).toEqual(testVendorId);

    await act(async () => {
      resolve(models);
    });

    expect(result.current.models).toEqual(models);
    expect(result.current.isLoading).toBeFalse();
  });
});
