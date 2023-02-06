/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { ReactNode } from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { applicationCtxStub } from 'neo/tests/stubs/contexts';
import { EPlatform } from 'neo/types/EPlatform';
import { ApplicationCtx } from 'neo/contexts/ApplicationCtx';
import { dataSourceCtxStub } from 'mg/tests/stubs/contexts';
import { DataSourceCtx } from 'neo/contexts/DataSourceCtx';
import { IProps, TReactions } from 'mg/components/Reactions/Reactions.types';
import { REACTIONS_HOVER_ENTER_TIMEOUT } from 'mg/components/Reactions/Reactions.const';
import 'neo/tests/mocks/hooks/contexts/useApplicationCtx';
import { Reactions } from '../Reactions';
import { TAvailableTypes } from '../Reactions.types';

Enzyme.configure({ adapter: new Adapter() });

const delay = 50;

const counts: TReactions<TAvailableTypes, number> = {
  Haha: 123,
  Angry: 3,
  Bored: 100,
  Like: 60,
  Dislike: 12,
  Sad: 73,
  Wow: 911,
};

const wrap = (children: ReactNode, platform = EPlatform.PHONE) => {
  const dataSourceCtx = {
    ...dataSourceCtxStub,
    neo: {
      ...dataSourceCtxStub.neo,
      platform,
    },
  };

  return (
    <ApplicationCtx.Provider value={applicationCtxStub}>
      <DataSourceCtx.Provider value={dataSourceCtx}>
        {children}
      </DataSourceCtx.Provider>
    </ApplicationCtx.Provider>
  );
};

describe('Reactions (news)', () => {
  describe('Component', () => {
    let component: ReactWrapper;

    afterEach(() => {
      component?.unmount();
    });

    it('should render correctly', () => {
      const tree = mount(wrap(<Reactions />));

      expect(tree.html()).toMatchSnapshot();
    });

    it('should mix className', () => {
      const tree = mount(wrap(<Reactions className="mixedClass" />));

      expect(tree.html()).toMatchSnapshot();
    });

    it('should render correctly with counts', () => {
      const tree = mount(wrap(<Reactions counts={counts} />));

      expect(tree.html()).toMatchSnapshot();
    });

    it('should render correctly with counts and reaction', () => {
      const tree = mount(wrap(<Reactions counts={counts} reaction={'Haha'} />));

      expect(tree.html()).toMatchSnapshot();
    });

    it('should render popup when click', () => {
      const wrapper = component = mount<IProps>(wrap(<Reactions />));

      wrapper.find('button.mg-reactions__preview').simulate('click');

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should close popup when click outside', () => {
      const wrapper = component = mount<IProps>(wrap(<Reactions />));

      wrapper.find('button.mg-reactions__preview').simulate('click');
      wrapper.find('.mg-reactions__paranja').simulate('click');

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should select reaction', (done) => {
      const wrapper = component = mount<IProps>(wrap(<Reactions counts={counts} />));

      wrapper.find('button.mg-reactions__preview').simulate('click');
      wrapper.find('button.mg-reactions__button-icon .mg-reactions__icon_reaction_Wow').simulate('click');

      setTimeout(() => {
        expect(wrapper.html()).toMatchSnapshot();

        setImmediate(done);
      }, delay * 3);
    });

    it('should unselect selected reaction', (done) => {
      const wrapper = component = mount<IProps>(wrap(<Reactions counts={counts} reaction={'Haha'} />));

      wrapper.find('button.mg-reactions__preview').simulate('click');

      setTimeout(() => {
        expect(wrapper.html()).toMatchSnapshot();

        setImmediate(done);
      }, delay * 3);
    });

    it('should render counts in popup', () => {
      const wrapper = component = mount<IProps>(wrap(<Reactions counts={counts} showCounts />));

      wrapper.find('button.mg-reactions__preview').simulate('click');

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should close popup after click in preview again', () => {
      const wrapper = component = mount<IProps>(wrap(<Reactions counts={counts} />));

      wrapper.find('button.mg-reactions__preview').simulate('click');
      wrapper.find('button.mg-reactions__preview').simulate('click');

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('should open popup on hover', (done) => {
      const wrapper = component = mount<IProps>(wrap(<Reactions counts={counts} />, EPlatform.DESKTOP));

      wrapper.find('button.mg-reactions__preview').simulate('mouseenter');

      setTimeout(() => {
        expect(wrapper.html()).toMatchSnapshot();

        done();
      }, REACTIONS_HOVER_ENTER_TIMEOUT + 50);
    });
  });
});

// https://github.com/enzymejs/enzyme/issues/2073
const mockConsoleMethod = (realConsoleMethod: any) => {
  const ignoredMessages = [
    'test was not wrapped in act(...)',
  ];

  return (message: string, ...args: any) => {
    const containsIgnoredMessage = ignoredMessages.some((ignoredMessage) => message.includes(ignoredMessage));

    if (!containsIgnoredMessage) {
      realConsoleMethod(message, ...args);
    }
  };
};

console.warn = jest.fn(mockConsoleMethod(console.warn));
console.error = jest.fn(mockConsoleMethod(console.error));

window.fetch = jest.fn().mockImplementation(() => (
  new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve({}),
        headers: {
          get: () => '',
        },
      });
    }, delay);
  })
));
