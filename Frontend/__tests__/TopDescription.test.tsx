import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { ApplicationCtx } from 'neo/contexts/ApplicationCtx';
import { applicationCtxStub } from 'neo/tests/stubs/contexts';
import { TopDescription } from '../TopDescription';

Enzyme.configure({ adapter: new Adapter() });

describe('TopDescription', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('should mount correctly', () => {
    component = mount(
      <ApplicationCtx.Provider value={applicationCtxStub}>
        <TopDescription />
      </ApplicationCtx.Provider>,
    );

    expect(component.html()).toMatchSnapshot();
  });

  it('should open correctly', () => {
    component = mount(
      <ApplicationCtx.Provider value={applicationCtxStub}>
        <TopDescription />
      </ApplicationCtx.Provider>,
    );
    component.find('.news-top-description__mode-toggler').simulate('click');

    expect(component.html()).toMatchSnapshot();
  });
});
