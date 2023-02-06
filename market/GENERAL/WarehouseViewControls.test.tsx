import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { WarehouseViewControls } from './WarehouseViewControls';
import { rootReducer } from 'src/store/root/reducer';
import { loadWarehousesAction, warehousesViewFilterAction } from 'src/store/root/warehouses/warehouses.actions';
import { WarehouseUsingType } from 'src/java/definitions';
import { Button, Select, SelectOption } from 'src/components';
import { twoTestWarehouses } from 'src/test/data';
import { normalizeWarehouses } from 'src/store/root/warehouses/warehouses.utils';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('<WarehouseViewControls/>', () => {
  let wrapper: ReactWrapper | null;
  store.dispatch(loadWarehousesAction.done({ result: normalizeWarehouses(twoTestWarehouses) }));

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('should render active category', () => {
    store.dispatch(
      warehousesViewFilterAction({
        ids: [],
        usingTypes: [WarehouseUsingType.USE_FOR_CROSSDOCK, WarehouseUsingType.USE_FOR_FULFILLMENT],
        activeTypes: [WarehouseUsingType.USE_FOR_FULFILLMENT],
      })
    );

    wrapper = mount(
      <Wrapper>
        <WarehouseViewControls warehousesIds={[]} />
      </Wrapper>
    );

    const buttons = wrapper.find(Button);
    expect(buttons.first().getElement().props.checked).toBeFalsy();
    expect(buttons.at(1).getElement().props.checked).toBeTruthy();
  });

  it('should select warehouse', () => {
    store.dispatch(
      warehousesViewFilterAction({
        ids: [],
        usingTypes: [WarehouseUsingType.USE_FOR_FULFILLMENT],
        activeTypes: [],
      })
    );

    wrapper = mount(
      <Wrapper>
        <WarehouseViewControls warehousesIds={[]} />
      </Wrapper>
    );

    const select = wrapper.find(Select).first();
    const selectOptions: SelectOption[] = select.prop('options');
    select.props().onChange([selectOptions[0]]);

    expect(store.getState().warehouses.viewFilter.ids[0]).toEqual(twoTestWarehouses[0].id);
  });
});
