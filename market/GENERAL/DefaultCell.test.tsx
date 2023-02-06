import React from 'react';
import { mount } from 'enzyme';

import { DefaultCell } from './DefaultCell';

describe('ReplenishmentDataGrid <DefaultCell />', () => {
  it('Should render', () => {
    const wrapper = mount(<DefaultCell value="testText" />);
    expect(wrapper.find(DefaultCell)).toBeDefined();
  });

  it('Should show value and title', () => {
    const text = 'testText';
    const wrapper = mount(<DefaultCell value={text} />);
    expect(wrapper.text()).toEqual(text);
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual(text);
  });

  it('Should show formatter value and title', () => {
    const text = <div>testText</div>;
    const wrapper = mount(<DefaultCell value={text} />);
    expect(wrapper.find(text)).toBeDefined();
    expect(wrapper.props().value).toEqual(text);
    expect(wrapper.find('div').at(0).props().title).toEqual('');
  });
});
