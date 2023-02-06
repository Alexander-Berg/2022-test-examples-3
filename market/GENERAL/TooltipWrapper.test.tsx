import { mount, ReactWrapper } from 'enzyme';
import React from 'react';
import { Popup } from '@yandex-lego/components/Popup/desktop/bundle';

import { TooltipWrapper } from '.';

let wrapper: ReactWrapper | undefined;
const defaultOptions = {
  content: 'TestContent',
  action: 'hover' as const,
};

describe('TooltipWrapper', () => {
  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
      wrapper = undefined;
    }
  });

  it('render', () => {
    wrapper = mount(<TooltipWrapper {...defaultOptions}>TestAnchor</TooltipWrapper>);

    expect(wrapper.find('.TooltipWrapper').length).toEqual(1);
    expect(wrapper.find('.TooltipWrapper-Anchor').length).toEqual(1);
    expect(wrapper.find(Popup).length).toEqual(0);
  });

  it('anchor', () => {
    wrapper = mount(<TooltipWrapper {...defaultOptions}>TestAnchor</TooltipWrapper>);

    expect(wrapper.find('.TooltipWrapper-Anchor').text()).toEqual('TestAnchor');
  });

  it('hover', () => {
    wrapper = mount(
      <TooltipWrapper {...defaultOptions} action="hover">
        TestAnchor
      </TooltipWrapper>
    );

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseEnter');

    expect(wrapper.find(Popup)).toHaveLength(1);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseLeave');

    expect(wrapper.find(Popup)).toHaveLength(0);
  });

  it('content', () => {
    wrapper = mount(<TooltipWrapper {...defaultOptions}>TestAnchor</TooltipWrapper>);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseEnter');

    expect(wrapper.find('.TooltipWrapper-Content').text()).toEqual('TestContent');
  });

  it('click', () => {
    wrapper = mount(
      <TooltipWrapper {...defaultOptions} action="click">
        TestAnchor
      </TooltipWrapper>
    );

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('click');

    expect(wrapper.find(Popup)).toHaveLength(1);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('click');

    expect(wrapper.find(Popup)).toHaveLength(0);
  });

  it('props visible', () => {
    wrapper = mount(
      <TooltipWrapper {...defaultOptions} popupProps={{ visible: false }}>
        TestAnchor
      </TooltipWrapper>
    );

    expect(wrapper.find(Popup)).toHaveLength(0);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseEnter');

    expect(wrapper.find(Popup)).toHaveLength(0);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('click');

    expect(wrapper.find(Popup)).toHaveLength(0);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseLeave');

    expect(wrapper.find(Popup)).toHaveLength(0);

    wrapper.setProps({ ...defaultOptions, popupProps: { visible: true } });
    wrapper.update();

    expect(wrapper.find(Popup)).toHaveLength(1);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('click');

    expect(wrapper.find(Popup)).toHaveLength(1);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseLeave');

    expect(wrapper.find(Popup)).toHaveLength(1);
  });

  it('hidden flag work', () => {
    wrapper = mount(
      <TooltipWrapper {...defaultOptions} hidden>
        TestAnchor
      </TooltipWrapper>
    );

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('mouseEnter');

    expect(wrapper.find(Popup)).toHaveLength(0);

    wrapper
      .find('.TooltipWrapper-Anchor')
      .first()
      .simulate('click');

    expect(wrapper.find(Popup)).toHaveLength(0);
  });
});
