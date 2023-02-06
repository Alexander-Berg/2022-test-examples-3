import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { testDisplayMsku, twoTestWarehouses } from 'src/test/data';
import { AvailabilityContext, AvailabilityContextValue } from '../../context';
import { WarehouseCell } from './WarehouseCell';
import { ShopSkuRow } from './ShopSkuRow';
import { MarketSkuCell } from 'src/components';
import { ShopSkuAvailabilityTable } from '.';
import { OfferAvailability } from 'src/java/definitions';

const store = createStore(rootReducer);
const Wrapper: FC<{ context: AvailabilityContextValue }> = ({ children, context }) => {
  return (
    <Provider store={store}>
      <AvailabilityContext.Provider value={context}>{children}</AvailabilityContext.Provider>
    </Provider>
  );
};

function getDefaultAvailabilityContext(overridded: Partial<AvailabilityContextValue> = {}) {
  const context: AvailabilityContextValue = {
    getCategoryLink: jest.fn(id => `category-link-${id}`),
    makeGetChangedAvailabilitySelector: () => () => undefined,
    makeCheckAvailabilityIsChoosedSelector: () => () => false,
    makeCheckAllWarehouseAvailabilitiesAreChoosedSelector: () => () => false,
    handleWarehouseMatrixAudit: () => undefined,
    getBatchChangesSelector: () => ({}),
    handleBPSelect: () => undefined,
    makeGetChangedStatusSelector: () => () => undefined,
    getChoosedAvailabilitiesSelector: () => ({}),
    getChangedAvailabilitiesSelector: () => [],
    getBatchStatusChangesSelector: () => ({ availability: null, isChoosed: false }),
    getFilteredSskuCountSelector: () => 0,
    makeShopSkuByIdSelector: id => () => ({
      availabilities: {},
      hidings: [],
      msku: testDisplayMsku({ id: +id }),
      availabilitiesByWarehouseId: {},
      almostDeadstockSskuByWarehouseId: {},
      deadstockSskuByWarehouseId: {},
      inheritAvailabilities: {},
      shopSkuStatus: {
        shopSku: 'shopSku',
        supplierId: 1,
        status: OfferAvailability.ACTIVE,
      },
      offer: {
        businessOfferId: 1,
        title: 'title',
        shopSkuKey: { supplierId: 1, shopSku: 'shopSku' },
        shopSku: 'shopSku',
        businessId: 1,
        businessSkuKey: { businessId: 1, shopSku: 'shopSku' },
        supplierId: 1,
      },
    }),
    ...overridded,
  };

  return context;
}

describe('SskuAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<ShopSkuAvailabilityTable />', () => {
    xit('should be contains ShopSkuRow', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <ShopSkuAvailabilityTable
            availabilityBySupplierId={{ '': [] }}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            onChangeStatus={jest.fn()}
            onResetStatus={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(ShopSkuRow)).toHaveLength(2);
    });

    xit('should be contains WarehouseCell', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <ShopSkuAvailabilityTable
            availabilityBySupplierId={{ '': [] }}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            onChangeStatus={jest.fn()}
            onResetStatus={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(WarehouseCell)).toHaveLength(2);
    });

    xit('should be contains TitleCell', () => {
      const context = getDefaultAvailabilityContext();

      wrapper = mount(
        <Wrapper context={context}>
          <ShopSkuAvailabilityTable
            availabilityBySupplierId={{ '': [] }}
            onChangeAvailability={jest.fn()}
            onResetAvailability={jest.fn()}
            onChangeStatus={jest.fn()}
            onResetStatus={jest.fn()}
            warehouses={twoTestWarehouses}
          />
        </Wrapper>
      );

      expect(wrapper.find(MarketSkuCell)).toHaveLength(1);
    });
  });
});
