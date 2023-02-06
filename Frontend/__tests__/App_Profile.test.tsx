/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { ReactNode } from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import AppProfile from 'partners/components/App/_Profile/App_Profile.desktop';
import { ServerCtx } from 'neo/contexts/ServerCtx';
import { ApplicationCtx } from 'neo/contexts/ApplicationCtx';
import { DataSourceCtx } from 'neo/contexts/DataSourceCtx';
import { NotificationsProvider } from 'partners/components/Notifications';
import { MessagesProvider } from 'partners/components/Messages';
import { applicationCtxStub, serverCtxStub } from 'neo/tests/stubs/contexts';
import { dataSourceCtxStub as dataSourcePartners } from 'partners/tests/stubs/DataSourceCtx';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';

const dataSourceCtxStub = {
  ...dataSourcePartners,
};

Enzyme.configure({ adapter: new Adapter() });

const wrap = (children: ReactNode) => {
  return (
    <ServerCtx.Provider value={serverCtxStub}>
      <ApplicationCtx.Provider value={applicationCtxStub}>
        <DataSourceCtx.Provider value={dataSourceCtxStub}>
          <MessagesProvider>
            <NotificationsProvider>{children}</NotificationsProvider>
          </MessagesProvider>
        </DataSourceCtx.Provider>
      </ApplicationCtx.Provider>
    </ServerCtx.Provider>
  );
};

describe('AppIndex', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('should render correctly', () => {
    component = mount(wrap(<AppProfile />));

    expect(component.html()).toMatchSnapshot();
  });
});
