import React, { ReactNode } from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { applicationCtxStub } from 'neo/tests/stubs/contexts';
import { ApplicationCtx } from 'neo/contexts/ApplicationCtx';
import { ETarget } from 'neo/types/html';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import { SubrubricItems } from '../SubrubricItems.desktop';
import { IProps } from '../SubrubricItems.types';

Enzyme.configure({ adapter: new Adapter() });

const items = [{
  href: 'https://yandex.ru',
  name: 'Всё',
  pinned: true,
}, {
  href: 'https://yandex.ru/search?text=армия и оружие',
  name: 'Армия и оружие',
}, {
  href: 'https://yandex.ru/search?text=лисы',
  name: 'Лисы',
}, {
  href: 'https://yandex.ru/search?text=маленькие лисы',
  name: 'Маленькие лисы',
  active: true,
}, {
  href: 'https://yandex.ru/search?text=усы котов',
  name: 'Усы лисят',
}, {
  href: 'https://yandex.ru/search?text=толстые коты',
  name: 'Толстые лисы',
  target: ETarget.BLANK,
}, {
  href: 'https://yandex.ru/search?text=Очень-очень длинный текст ссылки',
  name: 'Очень-очень длинный текст ссылки',
}];

const wrap = (children: ReactNode) => (
  <ApplicationCtx.Provider value={applicationCtxStub}>{children}</ApplicationCtx.Provider>
);

describe.skip('SubrubricItems', () => {
  describe('Component', () => {
    let component: ReactWrapper;

    afterEach(() => {
      component?.unmount();
    });

    it('should render correctly', () => {
      const wrapper = component = mount<IProps>(wrap(<SubrubricItems />));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should mix className', () => {
      const wrapper = component = mount<IProps>(wrap(<SubrubricItems className="mixedClass" />));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should render correctly with items', () => {
      const wrapper = component = mount<IProps>(wrap(<SubrubricItems items={items} />));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should render correctly with default prevented', () => {
      const wrapper = component = mount<IProps>(wrap(<SubrubricItems items={items} defaultPrevented />));

      expect(wrapper.html()).toMatchSnapshot();
    });
  });
});
