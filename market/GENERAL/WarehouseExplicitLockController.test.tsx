import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { loadWarehousesAction, warehousesViewFilterAction } from 'src/store/root/warehouses/warehouses.actions';
import { LockType, WarehouseUsingType } from 'src/java/definitions';
import { Select } from 'src/components';
import { twoTestWarehouses } from 'src/test/data';
import { normalizeWarehouses } from 'src/store/root/warehouses/warehouses.utils';
import { WarehouseExplicitLockController } from 'src/deepmind/components/WarehouseExplicitLockController/WarehouseExplicitLockController';

const store = createStore(rootReducer);
store.dispatch(loadWarehousesAction.done({ result: normalizeWarehouses(twoTestWarehouses) }));
store.dispatch(
  warehousesViewFilterAction({
    ids: [],
    usingTypes: [WarehouseUsingType.USE_FOR_FULFILLMENT],
    activeTypes: [WarehouseUsingType.USE_FOR_FULFILLMENT],
  })
);

const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('<WarehouseExplicitLockController/>', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('should render component', () => {
    wrapper = mount(
      <Wrapper>
        <WarehouseExplicitLockController value={{}} onChange={jest.fn()} />
      </Wrapper>
    );

    expect(wrapper.find(Select).length).toEqual(1);
  });

  it('should render warehouse selector', () => {
    const lockedAtWarehouseId = twoTestWarehouses[0].id;
    wrapper = mount(
      <Wrapper>
        <WarehouseExplicitLockController
          value={{ lockType: LockType.EXPLICIT_LOCK, lockedAtWarehouseId }}
          onChange={jest.fn()}
        />
      </Wrapper>
    );

    const select = wrapper.find(Select).last();
    expect(parseInt(select.prop('value').value, 10)).toEqual(lockedAtWarehouseId);
  });
});
