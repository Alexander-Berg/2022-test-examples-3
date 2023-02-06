import React from 'react';
import { ReactWrapper, mount } from 'enzyme';

import { DateIntervalForm } from './DateIntervalForm';

describe('MskuAvailabilityPages', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<DateIntervalForm />', () => {
    it('should contain fromDate in the component', () => {
      const fromDate = '2020-09-02';
      wrapper = mount(<DateIntervalForm fromDate={fromDate} onChangeFromDate={jest.fn()} onChangeToDate={jest.fn()} />);

      const input = wrapper.findWhere(n => n.key() === 'date-interval-start-date').find('input');

      expect(input.prop('value')).toBe('02.09.2020');
    });

    it('should contain toDate in the component', () => {
      const toDate = '2020-09-02';
      wrapper = mount(<DateIntervalForm toDate={toDate} onChangeFromDate={jest.fn()} onChangeToDate={jest.fn()} />);

      const input = wrapper.findWhere(n => n.key() === 'date-interval-end-date').find('input');

      expect(input.prop('value')).toBe('02.09.2020');
    });

    it('should call onChangeFromDate', () => {
      const handleChangeFromDate = jest.fn();
      const date = new Date(2020, 8, 2);
      wrapper = mount(<DateIntervalForm onChangeFromDate={handleChangeFromDate} onChangeToDate={jest.fn()} />);

      const datepicker = wrapper.findWhere(n => n.key() === 'date-interval-start-date');
      datepicker.prop('onChange')(date);

      expect(handleChangeFromDate).toBeCalledTimes(1);
      expect(handleChangeFromDate).toBeCalledWith('2020-09-02');
    });

    it('should call onChangeToDate', () => {
      const handleChangeToDate = jest.fn();
      const date = new Date(2020, 8, 2);
      wrapper = mount(<DateIntervalForm onChangeFromDate={jest.fn()} onChangeToDate={handleChangeToDate} />);

      const datepicker = wrapper.findWhere(n => n.key() === 'date-interval-end-date');
      datepicker.prop('onChange')(date);

      expect(handleChangeToDate).toBeCalledTimes(1);
      expect(handleChangeToDate).toBeCalledWith('2020-09-02');
    });
  });
});
