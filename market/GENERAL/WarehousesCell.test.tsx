import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import React from 'react';

import { rootReducer } from 'src/store/root/reducer';
import * as selectors from 'src/store/root/warehouses/warehouses.selectors';
import { twoTestWarehouses } from 'src/test/data';
import { WrapperWithStore } from 'src/pages/promo/tests/WrapperWithStore';
import { DIRECT_DISCOUNT_OFFER_MOCK } from 'src/pages/promo/tests/mocks/offer';
import { WarehousesCell } from './WarehousesCell';

const store = createStore(rootReducer);

describe('<WarehousesCell />', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
    jest.clearAllMocks();
  });

  const renderComponent = () => {
    wrapper = mount(
      <WrapperWithStore store={store}>
        <WarehousesCell offer={DIRECT_DISCOUNT_OFFER_MOCK} />
      </WrapperWithStore>
    );
  };

  function getWarehousesCellComponent() {
    return wrapper!.find(WarehousesCell);
  }

  beforeEach(() => {
    jest.spyOn(selectors, 'getWarehousesByIdSelector').mockImplementation(() =>
      twoTestWarehouses.reduce((acc, warehouse) => {
        acc[warehouse.id] = warehouse;
        return acc;
      }, {})
    );
    renderComponent();
  });

  it('should be render without errors', () => {
    expect(getWarehousesCellComponent()).toHaveLength(1);
  });

  it('should contain list of promo warehouses', () => {
    const warehousesItems = getWarehousesCellComponent().find('div > div');

    expect(warehousesItems).toHaveLength(2);
    expect(warehousesItems.at(0).text()).toEqual('100 wh100 - 123');
    expect(warehousesItems.at(1).text()).toEqual('200 wh200 - 321');
  });
});
