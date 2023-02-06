/* eslint-disable @typescript-eslint/no-explicit-any */
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { serverCtxStub } from 'neo/tests/stubs/contexts';
import { EBrowser, EOS } from 'neo/types/browser';
import { IServerCtx } from 'neo/types/contexts';
import { ServerCtx } from 'neo/contexts/ServerCtx';
import React, { ReactNode } from 'react';
import { EPolyfillStrategy } from 'neo/types/polyfills/EPolyfillStrategy';
import { Polyfills } from 'neo/core/components/Polyfills/Polyfills';

Enzyme.configure({ adapter: new Adapter() });

interface IGetServerCtxArgs {
  browser?: EBrowser;
  browserVersion?: string;
  browserBase?: EBrowser;
  browserBaseVersion?: string;
  os?: EOS, osVersion?: string;
  approach?: EPolyfillStrategy.DEFAULT;
}

function getServerCtx(args: IGetServerCtxArgs): IServerCtx {
  const {
    browser,
    browserVersion,
    browserBase,
    browserBaseVersion,
    os,
    osVersion,
    approach = EPolyfillStrategy.DEFAULT,
  } = args;

  return {
    neo: {
      ...serverCtxStub.neo,
      browserInfo: {
        os: {
          family: os,
          version: osVersion,
        },
        browser: {
          name: browser,
          version: browserVersion,
        },
        browserBase: {
          name: browserBase,
          version: browserBaseVersion,
        },
      },
      flags: {
        yxneo_polyfills: approach,
      },
    },
  };
}

const wrap = (children: ReactNode, serverCtx: IServerCtx): JSX.Element => {
  return (
    <ServerCtx.Provider value={serverCtx}>{children}</ServerCtx.Provider>
  );
};

describe('Polyfills', () => {
  let component: ReactWrapper | undefined;

  afterEach(() => {
    component?.unmount();
    component = undefined;
  });

  describe('Edge', () => {
    it('12', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.edge, browserVersion: '12' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('13', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.edge, browserVersion: '13' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('14', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.edge, browserVersion: '14' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('15', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.edge, browserVersion: '15' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('16', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.edge, browserVersion: '16' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('85', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.edge, browserVersion: '85' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });
  });

  describe('Firefox', () => {
    it('16', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '16' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('17', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '17' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('24', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '24' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('25', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '25' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('31', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '31' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('32', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '32' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('34', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '34' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('35', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '35' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('43', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '43' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('44', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '44' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('49', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '49' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('50', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '50' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('81', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.firefox, browserVersion: '81' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });
  });

  describe('Chrome & Chrome Mobile', () => {
    it('1', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '1' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });

    it('41', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '41' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });

    it('42', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '42' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });

    it('43', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '43' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });

    it('45', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '45' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });

    it('55', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '55' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });

    it('58', () => {
      [EBrowser.chrome, EBrowser.chromeMobile].forEach((browser) => {
        const serverCtx = getServerCtx({ browser, browserVersion: '58' });
        const wrapper = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();

        wrapper.unmount();
      });
    });
  });

  describe('Safari', () => {
    it('6', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.safari, browserVersion: '6' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('9', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.safari, browserVersion: '9' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('10', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.safari, browserVersion: '10' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('10.1', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.safari, browserVersion: '10.1' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });

    it('12.1', () => {
      const serverCtx = getServerCtx({ browser: EBrowser.safari, browserVersion: '12.1' });
      const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

      expect(wrapper.html()).toMatchSnapshot();
    });
  });

  describe('Safari iOS', () => {
    it('6', () => {
      ['5', '6', '7', '8', '9', '10', '11', '12', '13'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.safari,
          browserVersion: '6',
          os: EOS.ios,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('8', () => {
      ['5', '6', '7', '8', '9', '10', '11', '12', '13'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.safari,
          browserVersion: '8',
          os: EOS.ios,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('9', () => {
      ['5', '6', '7', '8', '9', '10', '11', '12', '13'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.safari,
          browserVersion: '9',
          os: EOS.ios,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('10', () => {
      ['5', '6', '7', '8', '9', '10', '11', '12', '13'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.safari,
          browserVersion: '10',
          os: EOS.ios,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('10.3', () => {
      ['5', '6', '7', '8', '9', '10', '11', '12', '13'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.safari,
          browserVersion: '10.3',
          os: EOS.ios,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('12.2', () => {
      ['5', '6', '7', '8', '9', '10', '11', '12', '13'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.safari,
          browserVersion: '12.2',
          os: EOS.ios,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });
  });

  describe('samsung', () => {
    it('4', () => {
      ['4', '4.4.4', '5', '6', '7', '8', '9', '10', '11', '12'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.samsung,
          browserVersion: '4',
          os: EOS.android,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('5', () => {
      ['4', '4.4.4', '5', '6', '7', '8', '9', '10', '11', '12'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.samsung,
          browserVersion: '5',
          os: EOS.android,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('6', () => {
      ['4', '4.4.4', '5', '6', '7', '8', '9', '10', '11', '12'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.samsung,
          browserVersion: '6',
          os: EOS.android,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('7', () => {
      ['4', '4.4.4', '5', '6', '7', '8', '9', '10', '11', '12'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.samsung,
          browserVersion: '7',
          os: EOS.android,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });

    it('7.2', () => {
      ['4', '4.4.4', '5', '6', '7', '8', '9', '10', '11', '12'].forEach((osVersion) => {
        const serverCtx = getServerCtx({
          browser: EBrowser.samsung,
          browserVersion: '7.2',
          os: EOS.android,
          osVersion,
        });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });
  });

  describe('IE', () => {
    it('9-11', () => {
      ['9', '10', '11'].forEach((browserVersion) => {
        const serverCtx = getServerCtx({ browser: EBrowser.ie, browserVersion });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });
  });

  describe('YandexSearch', () => {
    it('android', () => {
      ['1', '41', '42', '43', '45', '55', '58', '92'].forEach((browserBaseVersion) => {
        const serverCtx = getServerCtx({ browser: EBrowser.yandexSearch, os: EOS.android, browserBaseVersion });
        const wrapper = component = mount(wrap(<Polyfills />, serverCtx));

        expect(wrapper.html()).toMatchSnapshot();
      });
    });
  });
});
