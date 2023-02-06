import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { EmptyContent } from './EmptyContent';

describe('components', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<EmptyContent />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(<EmptyContent />);
      }).not.toThrow();
    });
  });
});
