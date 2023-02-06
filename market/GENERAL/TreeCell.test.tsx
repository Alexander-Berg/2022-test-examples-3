import React from 'react';
import { mount } from 'enzyme';

import { TreeCell } from './TreeCell';

describe('ReplenishmentDataGrid <TreeCell />', () => {
  it('Should render', () => {
    const wrapper = mount(<TreeCell value="testText" />);
    expect(wrapper.find(TreeCell)).toBeDefined();
  });

  it('Should show value and title', () => {
    const text = 'testText';
    const wrapper = mount(<TreeCell value={text} />);
    expect(wrapper.find('div.treeValue').at(0).text()).toEqual(text);
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual(text);
  });

  it('Should show parent icon', () => {
    const text = 'testText';
    const wrapper = mount(<TreeCell value={text} isChild={false} />);
    expect(wrapper.find('div.tree')).toBeDefined();
  });

  it('Should show child icon', () => {
    const text = 'testText';
    const wrapper = mount(<TreeCell value={text} isChild />);
    expect(wrapper.find('div.child')).toBeDefined();
  });

  it('Should show child icon click', () => {
    const text = 'testText';
    const clickMock = jest.fn();
    const wrapper = mount(<TreeCell value={text} isChild onClick={clickMock} />);
    wrapper
      .find('div.treeLine div')
      .at(0)
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    expect(clickMock.mock.calls.length).toEqual(0);
  });

  it('Should show parent icon click', () => {
    const text = 'testText';
    const clickMock = jest.fn();
    const wrapper = mount(<TreeCell value={text} isChild={false} onClick={clickMock} />);
    wrapper
      .find('div.treeLine div')
      .at(0)
      .simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    expect(clickMock.mock.calls.length).toEqual(1);
  });

  it('Should show formatter value and title', () => {
    const text = <i>testText</i>;
    const wrapper = mount(<TreeCell value={text} />);
    expect(wrapper.find(text)).toBeDefined();
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual('');
  });
});
