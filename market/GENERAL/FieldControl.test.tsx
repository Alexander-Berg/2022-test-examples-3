import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { FieldControl } from './FieldControl';

describe('components', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<FieldControl />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(<FieldControl label="Some label" />);
      }).not.toThrow();
    });

    it('should contains label', () => {
      wrapper = mount(<FieldControl label="Some label" />);

      expect(wrapper.contains('Some label')).toBeTruthy();
    });

    it('should contains children', () => {
      const div = <div>control</div>;
      wrapper = mount(<FieldControl label="Some label">{div}</FieldControl>);

      expect(wrapper.contains(div)).toBeTruthy();
    });
  });
});
