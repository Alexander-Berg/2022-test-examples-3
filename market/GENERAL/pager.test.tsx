import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { Pager } from './pager';
import { getPages } from './pager.utils';

let wrapper: ReactWrapper;
describe('<Pager />', () => {
  afterEach(() => {
    if (wrapper && wrapper.length) {
      wrapper.unmount();
    }
  });

  it('should be render without crashing', () => {
    expect(() => mount(<Pager current={1} total={10} onChange={jest.fn()} />)).not.toThrow();
  });

  it('should be called onChange', () => {
    const onChange = jest.fn();
    wrapper = mount(<Pager current={1} total={10} onChange={onChange} />);

    const button = wrapper.findWhere(node => {
      return node.text() === '5' && node.type() === 'button';
    });
    button.simulate('click');

    expect(onChange).toBeCalledTimes(1);
    expect(onChange.mock.calls[0][0]).toBe(5);
  });

  describe('getPages', () => {
    it('should be return a single element', () => {
      expect(getPages(1, 1, 9)).toEqual([1]);
    });

    it('should be return pages with start spacer', () => {
      expect(getPages(10, 10, 7)).toEqual([1, null, 6, 7, 8, 9, 10]);
      expect(getPages(7, 10, 7)).toEqual([1, null, 6, 7, 8, 9, 10]);
    });

    it('should be return pages with end spacer', () => {
      expect(getPages(1, 10, 7)).toEqual([1, 2, 3, 4, 5, null, 10]);
      expect(getPages(4, 10, 7)).toEqual([1, 2, 3, 4, 5, null, 10]);
    });

    it('should be return pages with start and end spacer', () => {
      expect(getPages(5, 10, 7)).toEqual([1, null, 4, 5, 6, null, 10]);
      expect(getPages(6, 10, 7)).toEqual([1, null, 5, 6, 7, null, 10]);
    });
  });
});
