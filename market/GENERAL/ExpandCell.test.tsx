import React from 'react';
import { mount } from 'enzyme';

import { ExpandCell } from './ExpandCell';

describe('ReplenishmentDataGrid <ExpandCell />', () => {
  it('Should render', () => {
    const text = 'testText';
    const wrapper = mount(<ExpandCell value={text} />);
    expect(wrapper.find(ExpandCell)).toBeDefined();
  });

  it('Should show value and title', () => {
    const text = 'testText';
    const wrapper = mount(<ExpandCell value={text} />);
    expect(wrapper.text()).toEqual(text);
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual(text);
  });

  it('Should show formatter value and title', () => {
    const text = <div>testText</div>;
    const wrapper = mount(<ExpandCell value={text} />);
    expect(wrapper.find(text)).toBeDefined();
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual('');
  });

  it('Should show formatter null data', () => {
    const text = '';
    const wrapper = mount(<ExpandCell value={text} />);
    expect(wrapper.html()).toBe('');
  });

  it('Click', () => {
    const text = 'testText';
    const clickMock = jest.fn();
    const wrapper = mount(<ExpandCell value={text} onClick={clickMock} />);
    wrapper.simulate('click', { stopPropagation: () => undefined, preventDefault: () => undefined });
    expect(clickMock.mock.calls.length).toEqual(1);
  });
});
