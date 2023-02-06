import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { MarketSkuStatusCell } from 'src/components';
import { testDisplayMsku, twoTestWarehouses } from 'src/test/data';
import { PageContext, PageContextValue } from '../../context';
import { WarehouseCell } from './WarehouseCell';
import { MarketSkuRow } from './MarketSkuRow';
import { TitleCell } from './TitleCell';
import { MarketSkuAvailabilityTable } from '.';
import { WarehouseUsingType } from 'src/java/definitions';
import { exportMskuTableToExcel } from 'src/store/root/market-sku/market-sku.actions';

const store = createStore(rootReducer);
const Wrapper: FC<{ context: PageContextValue }> = ({ children, context }) => {
  return (
    <Provider store={store}>
      <PageContext.Provider value={context}>{children}</PageContext.Provider>
    </Provider>
  );
};

function getDefaultAvailabilityContext(overridded: Partial<PageContextValue> = {}) {
  const context: PageContextValue = {
    usingType: WarehouseUsingType.USE_FOR_FULFILLMENT,
    exportTableAction: exportMskuTableToExcel.fulfilment,
    getCategoryLink: jest.fn(id => `category-link-${id}`),
    makeGetChangedAvailabilitySelector: () => () => undefined,
    makeCheckAvailabilityIsChoosedSelector: () => () => false,
    makeCheckAllWarehouseAvailabilitiesAreChoosedSelector: () => () => false,
    getBatchChangesSelector: () => ({}),
    getChangedAvailabilitiesSelector: () => [],
    getChoosedAvailabilitiesSelector: () => ({}),
    getFilteredMskuCountSelector: () => 0,
    makeMarketSkuByIdSelector: id => () => ({
      ...testDisplayMsku({ id }),
      availabilitiesByWarehouseId: {},
      inheritAvailabilitiesByWarehouseId: {},
      blockedSskuByWarehouseId: {},
    }),
    handleWarehouseMatrixAudit: jest.fn(),
    ...overridded,
  };

  return context;
}

describe('MskuAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<MarketSkuAvailabilityTable />', () => {
    it('should be contains MarketSkuRow', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <MarketSkuAvailabilityTable
            filter={{}}
            marketSkuIds={[1, 2]}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(MarketSkuRow)).toHaveLength(2);
    });

    it('should be contains WarehouseCell', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <MarketSkuAvailabilityTable
            filter={{}}
            marketSkuIds={[1]}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(WarehouseCell)).toHaveLength(2);
    });

    it('should be contains TitleCell', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <MarketSkuAvailabilityTable
            filter={{}}
            marketSkuIds={[1]}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(TitleCell)).toHaveLength(1);
    });

    it('should be hidden MarketSkuStatusCell', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <MarketSkuAvailabilityTable
            filter={{}}
            marketSkuIds={[1]}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(MarketSkuStatusCell)).toHaveLength(0);
    });

    it('should be contains MarketSkuStatusCell', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <MarketSkuAvailabilityTable
            filter={{}}
            showMskuStatuses
            marketSkuIds={[1]}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(MarketSkuStatusCell)).toHaveLength(1);
    });
  });
});
