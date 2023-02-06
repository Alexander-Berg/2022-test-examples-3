import { TreeBuilder } from 'src/components/TreeBuilder/TreeBuilder';
import { configure, mount, ReactWrapper } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import * as React from 'react';

configure({ adapter: new Adapter() });

const defaultProps = {
  hierarchyMap: {
    1: {
      id: 1,
      collapsed: true,
      children: [2, 3],
    },
    2: {
      id: 2,
      collapsed: true,
      parent: 1,
    },
    3: {
      id: 3,
      collapsed: true,
      parent: 1,
      children: [4],
    },
    4: {
      id: 4,
      collapsed: true,
      parent: 3,
    },
  },
  renderer: (id: number) => {
    return <div className="TestNode">{id}</div>;
  },
  onNodeToggle: () => {
    return undefined;
  },
};

let wrapper: ReactWrapper;

beforeEach(() => {
  wrapper = mount(<TreeBuilder {...defaultProps} />);
  wrapper.unmount();
});

describe('TreeBuilder', () => {
  it('one node', () => {
    wrapper.setProps(defaultProps);
    expect(wrapper.find('.TestNode').length).toEqual(1);
    expect(wrapper.find('.TreeBuilder-Node').length).toEqual(1);
    expect(wrapper.find('.TreeBuilder-NodeIcon').length).toEqual(1);
  });

  it('three nodes', () => {
    wrapper.setProps({
      hierarchyMap: {
        ...defaultProps.hierarchyMap,
        1: {
          ...defaultProps.hierarchyMap[1],
          collapsed: false,
        },
      },
    });
    expect(wrapper.find('.TestNode').length).toEqual(3);
    expect(wrapper.find('.TreeBuilder-Node').length).toEqual(3);
    expect(wrapper.find('.TreeBuilder-NodeIcon').length).toEqual(2);
  });

  it('arrow click', () => {
    let toggledId = 0;

    wrapper.setProps({
      onNodeToggle: (id: number) => {
        toggledId = id;
      },
    });
    wrapper.find('.icon').simulate('click');
    expect(toggledId).toEqual(1);
  });
});
