import { SearchTree } from 'src/components/SearchTree/SearchTree';
import { configure, mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import * as React from 'react';

configure({ adapter: new Adapter() });

const defaultProps = {
  hierarchyMap: {
    1: {
      id: 1,
      index: 0,
      text: 'node 1',
      collapsed: true,
      children: [2, 3],
    },
    2: {
      id: 2,
      index: 1,
      text: 'node 1-2',
      collapsed: true,
      parent: 1,
    },
    3: {
      id: 3,
      index: 2,
      text: 'node 1-3',
      collapsed: true,
      parent: 1,
      children: [4],
    },
    4: {
      id: 4,
      index: 3,
      text: 'node 1-3-4',
      collapsed: true,
      parent: 3,
    },
  },
  onSelect: () => {
    return undefined;
  },
  minKeyWordLength: 1,
  maxSuggestionsCount: 10,
};

const wrapper = mount(<SearchTree {...defaultProps} />);
wrapper.unmount();

beforeEach(() => {
  wrapper.setProps(defaultProps);
});

describe('SearchTree', () => {
  it('has rendered', () => {
    expect(wrapper.find('.Suggest').length).toEqual(1);
    expect(wrapper.find('.TreeBuilder').length).toEqual(1);
  });

  it('category collapse toggle check', () => {
    expect(wrapper.find('.SearchTree-Node').length).toEqual(1);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(3);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(1);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(3);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .at(1)
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(4);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .at(1)
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(3);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .at(1)
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(4);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(1);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
    expect(wrapper.find('.SearchTree-Node').length).toEqual(3);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
  });

  it('category click check', () => {
    let selectedNodeId = 0;
    wrapper.setProps({
      onSelect: (id: number) => {
        selectedNodeId = id;
      },
    });

    wrapper
      .find('.SearchTree-Node')
      .first()
      .simulate('click');
    expect(selectedNodeId).toEqual(1);
    expect(wrapper.find('.SearchTree-Node').length).toEqual(3);
    wrapper
      .find('.SearchTree-Node')
      .at(2)
      .simulate('click');
    expect(selectedNodeId).toEqual(3);
    expect(wrapper.find('.SearchTree-Node').length).toEqual(4);
    wrapper
      .find('.TreeBuilder-NodeIcon')
      .first()
      .simulate('click');
  });

  it('auto suggest check', () => {
    let selectedNodeId = 0;
    wrapper.setProps({
      onSelect: (id: number) => {
        selectedNodeId = id;
      },
    });
    wrapper
      .find('.textinput__control')
      .simulate('change', { target: { value: 'Hello' } })
      .simulate('focus');
    expect(
      (wrapper
        .find('.textinput__control')
        .first()
        .getDOMNode() as HTMLInputElement).value
    ).toEqual('Hello');
    expect(wrapper.find('.popup2_visible_yes').length).toEqual(0);
    wrapper.find('.textinput__control').simulate('change', { target: { value: '' } });
    expect(wrapper.find('.popup2_visible_yes').length).toEqual(0);
    wrapper.find('.textinput__control').simulate('change', { target: { value: '1' } });
    expect(wrapper.find('.popup2_visible_yes').length).toEqual(1);
    expect(wrapper.find('.menu__text').length).toEqual(4);
    wrapper
      .find('.menu__item')
      .at(2)
      .simulate('click');
    wrapper.find('.textinput__control').simulate('blur');
    expect(wrapper.find('.popup2_visible_yes').length).toEqual(0);
    expect(wrapper.find('.SearchTree-Node').length).toEqual(3);
    expect(selectedNodeId).toEqual(3);
  });
});
