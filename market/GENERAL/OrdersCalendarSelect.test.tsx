import { mount, ReactWrapper } from 'enzyme';
import React from 'react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import AsyncSelect from 'react-select/async-creatable';
import { act } from 'react-dom/test-utils';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { OrdersCalendarSelect } from './OrdersCalendarSelect';
import { rootReducer } from 'src/store/root/reducer';
import { ordersCalendar } from 'src/store/root/ordersCalendar';
import { ApiContext } from 'src/context';
import Api from 'src/Api';
import { setupApi } from 'src/test/setup';

let api: MockedApiObject<Api>;
const store = createStore(rootReducer);
store.dispatch(ordersCalendar.addName({ label: 'calendar', value: 1 }));

const setupFilter = (calendarWorkId: number | null) => {
  return mount(
    <MemoryRouter>
      <Provider store={store}>
        <ApiContext.Provider value={api}>
          <OrdersCalendarSelect rowId={0} calendarWorkId={calendarWorkId} onChange={jest.fn()} />
        </ApiContext.Provider>
      </Provider>
    </MemoryRouter>
  );
};

describe('<OrdersCalendarSelect />', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('should be render without errors', () => {
    expect(() => {
      wrapper = setupFilter(null);
    }).not.toThrow();
    expect(wrapper?.text()).toEqual('―');
  });

  it('should be render link', () => {
    wrapper = setupFilter(1);
    const link = wrapper.find('a');
    expect(link.text()).toEqual('calendar');
  });

  it('should create calendar', () => {
    api = setupApi();
    wrapper = setupFilter(null);
    const button = wrapper.find('button');
    button.simulate('click');
    const select = wrapper.find(AsyncSelect);
    act(() => select.props().onCreateOption?.('new_calendar'));
    expect(api.calendarWorkController.saveCalendar).toBeCalledTimes(1);
  });
});
