import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import 'neo/tests/mocks/hooks/contexts/useDataSourceCtx';
import { applicationCtxStub } from 'neo/tests/stubs/contexts';
import { ApplicationCtx } from 'neo/contexts/ApplicationCtx';
import { Image } from '../Image';

Enzyme.configure({
  adapter: new Adapter(),
});

describe('Image', () => {
  let ImageComponent: ReactWrapper;

  afterEach(() => {
    ImageComponent?.unmount();
  });

  it('should render correctly', () => {
    ImageComponent = mount((
      <ApplicationCtx.Provider value={applicationCtxStub}>
        <Image
          lazy={false}
          src="https://yastatic.net/iconostasis/_/8lFaTHLDzmsEZz-5XaQg9iTWZGE.png"
        />
      </ApplicationCtx.Provider>
    ));
    expect(ImageComponent.html()).toMatchSnapshot();
  });

  it('should render correctly (lazy)', () => {
    ImageComponent = mount((
      <ApplicationCtx.Provider value={applicationCtxStub}>
        <Image src="https://yastatic.net/iconostasis/_/8lFaTHLDzmsEZz-5XaQg9iTWZGE.png" />
      </ApplicationCtx.Provider>
    ));
    // Рисуется просто div, так как картинка не в области видимости
    expect(ImageComponent.html()).toMatchSnapshot();
  });
});
