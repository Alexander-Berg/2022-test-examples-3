import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { DisplayCargoType } from 'src/java/definitions';
import { CargoTypesList } from '.';

describe('components', () => {
  let wrapper: ReactWrapper | null;

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = null;
    }
  });

  describe('<CargoTypesList />', () => {
    it('should be render without errors', () => {
      expect(() => {
        wrapper = mount(<CargoTypesList cargoTypes={[]} />);
      }).not.toThrow();
    });

    it('should be render title', () => {
      wrapper = mount(<CargoTypesList cargoTypes={[]}>Some Title</CargoTypesList>);

      expect(wrapper.contains('Some Title')).toBe(true);
    });

    it('should be render items', () => {
      const cargoTypes: DisplayCargoType[] = [
        {
          lmsId: 1,
          description: 'one',
          mboParameterId: 10,
        },
        {
          lmsId: 2,
          description: 'two',
          mboParameterId: 20,
        },
      ];

      wrapper = mount(<CargoTypesList cargoTypes={cargoTypes} />);

      expect(wrapper.find('li')).toHaveLength(2);
    });
  });
});
