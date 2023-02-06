import { mount, ReactWrapper } from 'enzyme';
import React from 'react';

import { InputLine } from '.';

let wrapper: ReactWrapper;
const defaultOptions = {
  title: 'TestTitle',
};

beforeAll(() => {
  wrapper = mount(
    <InputLine {...defaultOptions}>
      <span>TestContent</span>
    </InputLine>
  );
  wrapper.unmount();
});

describe('InputLine', () => {
  it('render', () => {
    wrapper.setProps(defaultOptions);
    expect(wrapper.find('.InputLine').length).toEqual(1);
    expect(wrapper.find('.InputLine-Title').length).toEqual(1);
    expect(wrapper.find('.InputLine-Title').text()).toEqual('TestTitle');
    expect(wrapper.find('.InputLine > span').text()).toEqual('TestContent');
  });
});
