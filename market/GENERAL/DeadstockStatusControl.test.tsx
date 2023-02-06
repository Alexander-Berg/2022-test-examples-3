import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { DeadstockStatusControl } from '.';

describe('containers', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<MappingExistenceControl />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(<DeadstockStatusControl />);
      }).not.toThrow();
    });
  });
});
