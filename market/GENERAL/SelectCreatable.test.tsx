import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { SelectCreatable } from './SelectCreatable';

let wrapper: ReactWrapper | null;

describe('<SelectCreatable />', () => {
  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('should render without error', () => {
    expect(() => {
      wrapper = mount(<SelectCreatable options={[]} />);
    }).not.toThrow();
  });
});
