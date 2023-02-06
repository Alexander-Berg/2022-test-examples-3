import React, { useCallback, useRef, useState } from 'react';
import { compose } from '@bem-react/core';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { Popup as MgPopup } from 'mg/components/Popup/Popup.desktop';
import { IProps } from 'mg/components/Popup/Popup.types';
import { withTargetAnchor } from '@yandex-lego/components/Popup/desktop';
import { Button } from '@yandex-lego/components/Button';

Enzyme.configure({ adapter: new Adapter() });

const Popup = compose(withTargetAnchor)(MgPopup);
const PopupWithAnchor = (props: IProps): JSX.Element => {
  const rootRef = useRef(null);
  const anchorRef = useRef(null);

  const [visible, setVisible] = useState(false);

  const onClick = useCallback(() => {
    setVisible(!visible);
  }, [visible, setVisible]);

  return (
    <div ref={rootRef}>
      <Button onClick={onClick} ref={anchorRef}>
        Open popup
      </Button>

      <Popup
        theme="normal"
        view="mg"
        visible={visible}
        target="anchor"
        anchor={anchorRef}
        scope={rootRef}
        direction={['bottom-start', 'bottom']}
        {...props}
      >
        <span>Popup content</span>
      </Popup>
    </div>
  );
};

describe.skip('MgPopup', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('should mount correctly', () => {
    component = mount(<PopupWithAnchor />);

    expect(component.html()).toMatchSnapshot();
  });

  it('should open correctly', () => {
    component = mount(<PopupWithAnchor />);
    component.find(Button).simulate('click');

    expect(component.html()).toMatchSnapshot();
  });

  it('should close correctly', () => {
    component = mount(<PopupWithAnchor />);
    component.find(Button).simulate('click');
    component.find(Button).simulate('click');

    expect(component.html()).toMatchSnapshot();
  });

  it('should have tail', () => {
    component = mount(
      <PopupWithAnchor
        hasTail
        tailSize={26}
      />,
    );
    component.find(Button).simulate('click');

    expect(component.html()).toMatchSnapshot();
  });
});
