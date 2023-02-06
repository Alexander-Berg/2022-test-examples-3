import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { Popup2 } from '@/components/popup2';

let wrapper: ReactWrapper;

describe('<Popup2 />', () => {
  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  it('should render content if visible prop is true', () => {
    const popupContent = <span>popup</span>;
    const visible = true;

    function PopupWrapper() {
      const anchor = React.useRef<HTMLSpanElement>(null);

      return (
        <>
          <span ref={anchor}>anchor</span>
          <Popup2 anchor={anchor} visible={visible}>
            {popupContent}
          </Popup2>
        </>
      );
    }

    wrapper = mount(<PopupWrapper />);

    expect(wrapper.contains(popupContent)).toBe(true);
  });

  it('should not render content if visible prop is false', () => {
    const popupContent = <span>popup</span>;
    const visible = false;

    function PopupWrapper() {
      const anchor = React.useRef<HTMLSpanElement>(null);

      return (
        <>
          <span ref={anchor}>anchor</span>
          <Popup2 anchor={anchor} visible={visible}>
            {popupContent}
          </Popup2>
        </>
      );
    }

    wrapper = mount(<PopupWrapper />);

    expect(wrapper.contains(popupContent)).toBe(false);
  });

  it('should render content if keepMounted prop is passed', () => {
    const popupContent = <span>popup</span>;
    const visible = false;

    function PopupWrapper() {
      const anchor = React.useRef<HTMLSpanElement>(null);

      return (
        <>
          <span ref={anchor}>anchor</span>
          <Popup2 anchor={anchor} visible={visible} keepMounted>
            {popupContent}
          </Popup2>
        </>
      );
    }

    wrapper = mount(<PopupWrapper />);

    expect(wrapper.contains(popupContent)).toBe(true);
  });

  it('should render tail if hasTail prop is passed', () => {
    const popupContent = <span>popup</span>;

    function PopupWrapper() {
      const anchor = React.useRef<HTMLSpanElement>(null);

      return (
        <>
          <span ref={anchor}>anchor</span>
          <Popup2 anchor={anchor} visible hasTail>
            {popupContent}
          </Popup2>
        </>
      );
    }

    wrapper = mount(<PopupWrapper />);

    expect(wrapper.find(Popup2).getDOMNode().childNodes).toHaveLength(2);
  });

  it('should render in custom portal container', () => {
    const container = document.createElement('div');

    function PopupWrapper() {
      const anchor = React.useRef<HTMLSpanElement>(null);

      return (
        <>
          <span ref={anchor}>anchor</span>
          <Popup2 anchor={anchor} visible container={container}>
            <span>popup</span>
          </Popup2>
        </>
      );
    }

    wrapper = mount(<PopupWrapper />);

    expect(container.childNodes).toHaveLength(1);
  });
});
