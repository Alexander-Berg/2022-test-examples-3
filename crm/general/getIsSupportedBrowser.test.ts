import { getIsSupportedBrowser } from './getIsSupportedBrowser';
import { config } from '../Config';

const Chrome =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 Safari/537.36';

const Safari =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 12_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15';

const YandexV22 =
  'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.63 YaBrowser/22.5.0 Yowser/2.5 Safari/537.36';

const YandexV13 =
  'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.12785 YaBrowser/13.12.1599.12785 Safari/537.36';

const FirefoxV64 = 'Mozilla/5.0 (X11; Linux x86_64; rv:54.0) Gecko/20100101 Firefox/64.0';
const FirefoxV68 = 'Mozilla/5.0 (Windows NT 6.1; rv:68.7) Gecko/20100101 Firefox/68.7';
const FirefoxV101 =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:101.0) Gecko/20100101 Firefox/101.0';

const EdgeV18 =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.19582';

const EdgeV94 =
  'Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4758.102 Safari/537.36 Edg/94.0.1150.39';

const Avant =
  'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; Avant Browser; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)';

const MobileSafari =
  'Mozilla/5.0 (iPad; CPU OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko ) Version/5.1 Mobile/9B176 Safari/7534.48.3';

const OperaMobi =
  'Opera/12.02 (Android 4.1; Linux; Opera Mobi/ADR-1111101157; U; en-US) Presto/2.9.201 Version/12.02';

const setUA = (ua: string) => {
  Object.defineProperty(window.navigator, 'userAgent', { get: () => ua, configurable: true });
};

describe('getIsSupportedBrowser', () => {
  describe('when browser is not supported', () => {
    describe('because of type:', () => {
      it.each`
        name               | ua
        ${'Chrome'}        | ${Chrome}
        ${'Safari'}        | ${Safari}
        ${'Mobile Safari'} | ${MobileSafari}
        ${'Opera Mobi'}    | ${OperaMobi}
      `('blocks $name', ({ ua }) => {
        setUA(ua);
        expect(getIsSupportedBrowser()).toEqual(false);
      });
    });
    describe('because of version:', () => {
      it.each`
        name             | ua
        ${'Yandex v13'}  | ${YandexV13}
        ${'Firefox v64'} | ${FirefoxV64}
        ${'Edge v18'}    | ${EdgeV18}
      `('blocks $name', ({ ua }) => {
        setUA(ua);
        expect(getIsSupportedBrowser()).toEqual(false);
      });
    });
  });
  describe('when browser is supported', () => {
    describe('because of type:', () => {
      it('allows exotic', () => {
        setUA(Avant);
        expect(getIsSupportedBrowser()).toEqual(true);
      });
    });
    describe('because of version:', () => {
      it.each`
        name              | ua
        ${'Yandex v22'}   | ${YandexV22}
        ${'Firefox v68'}  | ${FirefoxV68}
        ${'Firefox v101'} | ${FirefoxV101}
        ${'Edge v94'}     | ${EdgeV94}
      `('allows $name', ({ ua }) => {
        setUA(ua);
        expect(getIsSupportedBrowser()).toEqual(true);
      });
    });
  });
});
