import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { CreatableMultiStringSelect } from '.';

describe('components', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<CreatableMultiStringSelect />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(<CreatableMultiStringSelect />);
      }).not.toThrow();
    });
  });
});
