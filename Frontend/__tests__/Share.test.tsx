/* eslint-disable @typescript-eslint/no-explicit-any */
import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import { Share } from '../Share';

Enzyme.configure({ adapter: new Adapter() });

describe('Share', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('should render correctly', () => {
    component = mount(<Share url="https://yandex.ru/news" description="Новости" />);

    expect(component.html()).toMatchSnapshot();
  });

  it('should open share popup', (done) => {
    component = mount(<Share url="https://yandex.ru/news" description="Новости" />);
    component.find('.mg-share').simulate('click');

    setTimeout(() => {
      expect(component.html()).toMatchSnapshot();
      done();
    });
  });
});

// @ts-ignore
global.Ya = {
  share2: jest.fn().mockImplementation(() => {}),
};
