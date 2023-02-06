import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import React from 'react';

import { rootReducer } from 'src/store/root/reducer';
import { warehouse1 } from 'src/test/data/warehouses';
import { WrapperWithStore } from 'src/pages/promo/tests/WrapperWithStore';
import { DIRECT_DISCOUNT_OFFER_MOCK } from 'src/pages/promo/tests/mocks/offer';
import { WarehouseCell } from './WarehouseCell';

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
        <WarehouseCell offer={DIRECT_DISCOUNT_OFFER_MOCK} warehouseId={`${warehouse1.id}`} />
      </WrapperWithStore>
    );
  };

  function getWarehouseCellComponent() {
    return wrapper!.find(WarehouseCell);
  }

  beforeEach(() => {
    renderComponent();
  });

  it('should be render without errors', () => {
    expect(getWarehouseCellComponent()).toHaveLength(1);
  });

  it('should contain warehouse details', () => {
    const warehousesItems = getWarehouseCellComponent().find('div');

    expect(warehousesItems).toHaveLength(1);
    expect(warehousesItems.at(0).text()).toEqual('123');
  });
});
