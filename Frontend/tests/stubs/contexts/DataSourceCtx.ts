import { IDataSourceCtx } from 'neo/types/contexts';
import { EPlatform } from 'neo/types/EPlatform';
import { EService } from 'neo/types/EService';
import { ETld } from 'neo/types/ETld';
import { EBrowser, EOS } from 'neo/types/browser';
import { ELanguage } from 'neo/types/ELanguage';
import { ETheme } from 'neo/types/ETheme';

export const dataSourceCtxStub: IDataSourceCtx = {
  neo: {
    platform: EPlatform.PHONE,
    service: EService.NEWS,
    page: 'main',
    reqid: 'reqid',
    tld: ETld.RU,
    browserInfo: {
      os: {
        family: EOS.android,
        version: '6.0',
      },
      browser: {
        name: EBrowser.chromeMobile,
        version: '83.0.4103',
      },
      browserBase: {
        name: EBrowser.chromium,
        version: '83.0.4103',
      },
    },
    isYandex: false,
    language: ELanguage.RU,
    locale: 'ru-RU',
    url: new URL('https://yandex.ru/news'),
    timestamp: 1590573235493,
    hash: 'qwe',
    advertConfig: {},
    theme: ETheme.LIGHT,
  },
};
