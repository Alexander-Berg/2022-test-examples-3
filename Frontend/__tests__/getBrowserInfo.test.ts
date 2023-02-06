import { INewsRequest } from 'mg/types/apphost/news_request';
import { getBrowserInfo } from '../getBrowserInfo';

describe('getBrowserInfo', () => {
  it('Возвращает undefined, если поле в INewsRequest не пришло', () => {
    const request = {
      detected_render: {
        detector: {
          OSVersion: '12.0.1',
          BrowserVersion: '12.3.1',
          BrowserBaseVersion: '12',
        },
      },
    } as INewsRequest;

    const result = getBrowserInfo(request);

    expect(result).toStrictEqual({
      os: {
        family: undefined,
        version: '12.0.1',
      },
      browser: {
        name: undefined,
        version: '12.3.1',
      },
      browserBase: {
        name: undefined,
        version: '12',
      },
    });
  });

  it('Возвращает значение полей в нижнем регистре', () => {
    const request = {
      detected_render: {
        detector: {
          OSFamily: 'Windows',
          BrowserName: 'FIREFOX',
          BrowserBase: 'FireFox',
          BrowserVersion: 'testBrowserVersion',
          OSVersion: 'testOSVersion',
          BrowserBaseVersion: 'testBrowserBaseVersion',
        },
      },
    } as unknown as INewsRequest;

    const result = getBrowserInfo(request);

    expect(result).toStrictEqual({
      os: {
        family: 'windows',
        version: 'testOSVersion',
      },
      browser: {
        name: 'firefox',
        version: 'testBrowserVersion',
      },
      browserBase: {
        name: 'firefox',
        version: 'testBrowserBaseVersion',
      },
    });
  });
});
