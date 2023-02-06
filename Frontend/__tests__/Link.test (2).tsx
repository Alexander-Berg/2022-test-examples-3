import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import { Link } from '../Link';

Enzyme.configure({
  adapter: new Adapter(),
});

describe('Link', () => {
  let LinkComponent: ReactWrapper;

  afterEach(() => {
    LinkComponent.unmount();
  });

  it('should render correctly', () => {
    LinkComponent = mount(<Link href="https://yandex.ru">Яндекс</Link>);
    expect(LinkComponent.html()).toMatchSnapshot();
  });
});
