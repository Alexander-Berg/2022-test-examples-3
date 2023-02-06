import { SuggestedModel, Vendor } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import React from 'react';
import { act } from 'react-dom/test-utils';
import { mount } from 'enzyme';

import { useSearchVendors } from 'src/shared/hooks';
import { Wrapper } from 'src/tasks/common-logs/test/utils/getLogsWrapper';
import { useLoadModels } from './ModelsBlock/useLoadModels';
import { ModelsBlock } from './ModelsBlock/ModelsBlock';
import { ModelSelectModal } from './ModelSelectModal';
import { VendorSelect } from '../../common-logs/components/VendorSelect/VendorSelect';

jest.mock('../../hooks/useSearchVendors');
jest.mock('./ModelsBlock/useLoadModels');
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

const useLoadModelsMock = useLoadModels as jest.Mock<{ models: SuggestedModel[]; isLoading: boolean }>;
const useSearchVendorsMock = useSearchVendors as jest.Mock<(query?: string) => Promise<Vendor[]>>;

describe('ModelSelectModal', () => {
  useSearchVendorsMock.mockReturnValue(() => Promise.resolve([]));
  useLoadModelsMock.mockReturnValue({ models: [], isLoading: false });

  it('renders loading', () => {
    const wrapper = mount(
      <Wrapper
        initialState={{
          vendors: {},
          ui: { loader: {} },
        }}
      >
        <ModelSelectModal categoryId={123} onClose={jest.fn()} onSelect={jest.fn()} visible />
      </Wrapper>
    );

    expect(wrapper.find(VendorSelect)).toHaveLength(1);
    expect(wrapper.find(ModelsBlock)).toHaveLength(1);
  });

  it('onSelect works', () => {
    const vendor = { vendor_id: 123 };
    const model = { id: 123 };

    let selectedVendorId;
    let selectedModel;

    const wrapper = mount(
      <Wrapper
        initialState={{
          vendors: {},
          ui: { loader: {} },
        }}
      >
        <ModelSelectModal
          categoryId={123}
          onClose={jest.fn()}
          onSelect={(vendorId, _model) => {
            selectedVendorId = vendorId;
            selectedModel = _model;
          }}
          vendorId={123}
          visible
        />
      </Wrapper>
    );

    act(() => {
      wrapper.find(VendorSelect).prop('onSelect')(vendor);
    });

    wrapper.update();

    act(() => {
      wrapper.find(ModelsBlock).prop('onModelSelect')(model);
    });

    expect(selectedVendorId).toEqual(123);
    expect(selectedModel).toEqual(model);
  });
});
