import { mount, ReactWrapper } from 'enzyme';
import React from 'react';

import { DatePicker, DatePickerProps } from '.';

type DatePickerWrapper = ReactWrapper<DatePickerProps>;

const defaultOptions: DatePickerProps = {
  inputProps: {},
  selected: new Date('2011/10/10'),
  onChange: () => undefined,
};

const setup = () => {
  const wrapper: DatePickerWrapper = mount(<DatePicker {...defaultOptions} />);
  const wrapperInline: DatePickerWrapper = mount(<DatePicker {...defaultOptions} inline />);

  return {
    wrapper,
    wrapperInline,
  };
};

const wrappers = setup();

describe('DatePicker', () => {
  it('render', () => {
    const { wrapper, wrapperInline } = wrappers;

    expect(wrapper.find('.DatePicker').hostNodes()).toHaveLength(0);
    expect(wrapper.find('.DatePicker-Control').hostNodes()).toHaveLength(1);

    expect(wrapperInline.find('.DatePicker').hostNodes()).toHaveLength(1);
    expect(wrapperInline.find('.DatePicker-Control').hostNodes()).toHaveLength(0);
  });

  xit('props value change', () => {
    const { wrapper } = wrappers;

    expect(
      wrapper
        .find('.DatePicker-Control input')
        .first()
        .prop('value')
    ).toEqual('10.10.2011');

    wrapper.setProps({ ...defaultOptions, selected: undefined });

    expect(
      wrapper
        .find('.DatePicker-Control input')
        .first()
        .prop('value')
    ).toEqual('');
  });

  it('input change', () => {
    const { wrapper } = wrappers;

    let date: Date | null | undefined;

    wrapper.setProps({
      ...defaultOptions,
      onChange: (value: Date) => {
        date = value;
      },
    });

    wrapper
      .find('.DatePicker-Control input')
      .first()
      .simulate('change', { target: { value: '15.05.2019' } });
    expect(date!.toISOString()).toEqual(new Date('2019/05/15').toISOString());

    wrapper
      .find('.DatePicker-Control input')
      .first()
      .simulate('change', { target: { value: '' } });
    expect(date).toEqual(null);
  });
});
