import React, { ReactNode } from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import { DataSourceCtx } from 'neo/contexts/DataSourceCtx';
import { dataSourceCtxStub as dataSourceCtxStubBase } from 'neo/tests/stubs/contexts';
import { LoadButton } from '../LoadMore.abstract';

Enzyme.configure({ adapter: new Adapter() });

const dataSourceCtxStub = {
  neo: {
    ...dataSourceCtxStubBase.neo,
  },
  mg: {
    user: {
      uid: {
        value: '1',
      },
    },
    isVertical: false,
    apiBaseUrl: 'apiBaseUrl',
  },
};

const wrap = (children: ReactNode) => {
  return (
    <DataSourceCtx.Provider value={dataSourceCtxStub}>
      {children}
    </DataSourceCtx.Provider>
  );
};

describe('LoadButton', () => {
  let component: ReactWrapper;

  afterEach(() => {
    component?.unmount();
  });

  it('should render correctly on different states', (done) => {
    const onLoad = jest.fn();
    let fetchUrl = '';
    // @ts-ignore
    window.fetch = jest.fn().mockImplementation((url) => {
      fetchUrl = url;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ data: {} }),
        headers: {
          get: () => '',
        },
      });
    });

    component = mount(wrap(<LoadButton nextPage="/nextPage" onLoad={onLoad} buttonText="load" />));

    expect(component.html()).toMatchSnapshot();
    component.find('button').simulate('click');
    expect(component.html()).toMatchSnapshot();

    setTimeout(() => {
      expect(fetchUrl).toBe('http://localhost/nextPage?neo_parent_id=reqid');
      expect(onLoad).toBeCalled();

      done();
    }, 0);
  });

  it('should render correctly when nextPage is empty', () => {
    // eslint-disable-next-line react/jsx-no-bind
    component = mount(wrap(<LoadButton onLoad={() => {}} buttonText="load" />));

    expect(component.html()).toMatchSnapshot();
  });
});
