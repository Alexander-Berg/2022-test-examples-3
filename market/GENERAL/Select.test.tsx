import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { Select } from './Select';

let wrapper: ReactWrapper | null;

describe('<Select />', () => {
  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  it('should render without error', () => {
    expect(() => {
      wrapper = mount(<Select options={[]} />);
    }).not.toThrow();
  });
});
