import { CategoryTree, CategoryTreeProps } from 'src/components/CategoryTree/CategoryTree';
import { configure, mount } from 'enzyme';
import * as React from 'react';
import Adapter from 'enzyme-adapter-react-16';

configure({ adapter: new Adapter() });

const defaultProps: CategoryTreeProps = {
  items: [
    {
      hid: 1,
      name: 'node 1',
      parentHid: -1,
    },
    {
      hid: 2,
      name: 'node 1-2',
      parentHid: 1,
    },
    {
      hid: 3,
      name: 'node 1-3',
      parentHid: 1,
    },
    {
      hid: 4,
      name: 'node 1-3-4',
      parentHid: 3,
    },
  ],
  onSelect: () => {
    return undefined;
  },
  minKeyWordLength: 1,
  maxSuggestionsCount: 10,
};

const wrapper = mount(<CategoryTree {...defaultProps} />);
wrapper.unmount();

beforeAll(() => {
  wrapper.setProps(defaultProps);
});

describe('CategoryTree', () => {
  it('has rendered', () => {
    expect(wrapper.find('.Suggest').length).toEqual(1);
    expect(wrapper.find('.TreeBuilder').length).toEqual(1);
  });
});
