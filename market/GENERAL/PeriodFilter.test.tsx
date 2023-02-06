import React, { FC } from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { PeriodEnum, PeriodFilter } from './PeriodFilter';
import { rootReducer } from 'src/store/root/reducer';
import { DatePicker, Select } from 'src/components';
import { OrdersCalendarFilterParams } from 'src/store/root/ordersCalendar';

const store = createStore(rootReducer);
const Wrapper: FC = ({ children }) => {
  return <Provider store={store}>{children}</Provider>;
};

describe('containers', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<PeriodFilter />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(
          <Wrapper>
            <PeriodFilter filter={{ year: 2020, calendarWorkId: 1 }} updateFilter={jest.fn()} />
          </Wrapper>
        );
      }).not.toThrow();
    });

    it('should be period changed', () => {
      let result: Partial<OrdersCalendarFilterParams> = {};
      const handleChangeFilter = (filter: Partial<OrdersCalendarFilterParams>) => {
        result = filter;
      };

      wrapper = mount(
        <Wrapper>
          <PeriodFilter filter={{ year: 2000, calendarWorkId: 1 }} updateFilter={handleChangeFilter} />
        </Wrapper>
      );

      wrapper.find(Select).first().prop('onChange')({ label: 'месяц', value: PeriodEnum.MONTH });
      expect(result.month).toEqual(new Date().getMonth() + 1);
    });

    it('should be date changed', () => {
      let result: Partial<OrdersCalendarFilterParams> = {};
      const handleChangeFilter = (filter: Partial<OrdersCalendarFilterParams>) => {
        result = filter;
      };

      wrapper = mount(
        <Wrapper>
          <PeriodFilter filter={{ year: 2000, calendarWorkId: 1 }} updateFilter={handleChangeFilter} />
        </Wrapper>
      );

      wrapper.find(DatePicker).prop('onChange')(new Date(2022, 1, 3));
      expect(result.year).toEqual(2022);
    });
  });
});
