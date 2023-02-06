import { getFooter } from 'news/lib/dataSource/getFooter';
import { ETld } from 'neo/types/ETld';
import { EPlatform } from 'neo/types/EPlatform';
import { IServerCtx } from 'news/types/contexts';
import { EPage } from 'news/types/EPage';
import { ICtxRR } from 'neo/types/report-renderer';

test('getFooter ru', () => {
  const request: IServerCtx = {
    neo: {
      platform: EPlatform.PHONE,
      tld: ETld.RU,
      flags: {},
    },
  } as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Обратная связь',
      target: '_blank',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Пользовательское соглашение',
      target: '_blank',
      url: 'https://yandex.ru/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      target: '_blank',
      url: 'https://yandex.ru/adv/',
    },
    {
      text: 'Информация для СМИ',
      target: '_blank',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
    {
      text: 'Сейчас в СМИ. Статистика',
      target: '_blank',
      url: 'https://yandex.ru/news/top',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);
});

test('getFooter ru region', () => {
  const ctxRR: Partial<ICtxRR> = {
    // @ts-ignore
    getItems: function getItems() {
      return [{
        handler: 'rubric',
        data: {
          rubric: {
            rubric: {
              id: 56,
            },
          },
        },
      }];
    },
  };

  const request: IServerCtx = {
    news: {
      isRegion: true,
      regionId: 56,
      rubricsInfo: {
        regions: {
          '56': {
            alias: 'Chelyabinsk',
            names: {
              ru: 'Челябинск',
            },
            country_id: 225,
          },
        },
      },
    },
    neo: {
      platform: EPlatform.PHONE,
      tld: ETld.RU,
      page: EPage.RUBRIC,
      ctxRR,
      flags: {},
    },
  } as unknown as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Обратная связь',
      target: '_blank',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Пользовательское соглашение',
      target: '_blank',
      url: 'https://yandex.ru/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      target: '_blank',
      url: 'https://yandex.ru/adv/',
    },
    {
      text: 'Информация для СМИ',
      target: '_blank',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
    {
      text: 'Сейчас в СМИ. Статистика',
      url: 'https://yandex.ru/news/top',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);

  request.neo.flags['yxneo_show-regions-top'] = '1';

  expect(getFooter(request)[4]).toMatchObject({
    text: 'Сейчас в СМИ: Челябинск. Статистика',
    url: 'https://yandex.ru/news/top/region/Chelyabinsk',
  });
});

test('getFooter ua', () => {
  const request: IServerCtx = {
    neo: {
      platform: EPlatform.PHONE,
      tld: ETld.UA,
      flags: {},
    },
  } as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Обратная связь',
      target: '_blank',
      url: 'https://yandex.ua/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Пользовательское соглашение',
      target: '_blank',
      url: 'https://yandex.ua/legal/news_termsofuse/?lang=uk',
    },
    {
      text: 'Реклама',
      target: '_blank',
      url: 'https://yandex.ru/adv/',
    },
    {
      text: 'Информация для СМИ',
      target: '_blank',
      url: 'https://yandex.ua/support/news/info-for-mass-media.html',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);
});

test('getFooter kz', () => {
  const request: IServerCtx = {
    neo: {
      platform: EPlatform.PHONE,
      tld: ETld.KZ,
      flags: {},
    },
  } as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Обратная связь',
      target: '_blank',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Пользовательское соглашение',
      target: '_blank',
      url: 'https://yandex.kz/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      target: '_blank',
      url: 'https://yandex.kz/adv/',
    },
    {
      text: 'Информация для СМИ',
      target: '_blank',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);
});

test('getFooter ru desktop', () => {
  const request: IServerCtx = {
    neo: {
      platform: EPlatform.DESKTOP,
      tld: ETld.RU,
      page: EPage.RUBRIC,
      flags: {},
    },
    news: {},
  } as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Информация для СМИ',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
    {
      text: 'База данных СМИ',
      url: 'https://yandex.ru/news/smi',
    },
    {
      text: 'Пользовательское соглашение',
      url: 'https://yandex.ru/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      url: 'https://yandex.ru/adv/',
    },
    {
      text: 'Обратная связь',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Сейчас в СМИ. Статистика',
      url: 'https://yandex.ru/news/top',
    },
    {
      text: 'Справка',
      url: 'https://yandex.ru/support/news/',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);
});

test('getFooter ru region desktop', () => {
  const ctxRR: Partial<ICtxRR> = {
    // @ts-ignore
    getItems: function getItems() {
      return [{
        handler: 'rubric',
        data: {
          rubric: {
            rubric: {
              id: 56,
            },
          },
        },
      }];
    },
  };

  const request: IServerCtx = {
    news: {
      isRegion: true,
      regionId: 56,
      rubricsInfo: {
        regions: {
          '56': {
            alias: 'Chelyabinsk',
            names: {
              ru: 'Челябинск',
            },
            country_id: 225,
          },
        },
      },
    },
    neo: {
      platform: EPlatform.DESKTOP,
      tld: ETld.RU,
      page: EPage.RUBRIC,
      ctxRR,
      flags: {},
    },
  } as unknown as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Информация для СМИ',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
    {
      text: 'База данных СМИ',
      url: 'https://yandex.ru/news/smi',
    },
    {
      text: 'Пользовательское соглашение',
      url: 'https://yandex.ru/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      url: 'https://yandex.ru/adv/',
    },
    {
      text: 'Обратная связь',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Сейчас в СМИ. Статистика',
      url: 'https://yandex.ru/news/top',
    },
    {
      text: 'Справка',
      url: 'https://yandex.ru/support/news/',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);

  request.neo.flags['yxneo_show-regions-top'] = '1';

  expect(getFooter(request)[5]).toMatchObject({
    text: 'Сейчас в СМИ: Челябинск. Статистика',
    url: 'https://yandex.ru/news/top/region/Chelyabinsk',
  });
});

test('getFooter ru non-russian region desktop', () => {
  const ctxRR: Partial<ICtxRR> = {
    // @ts-ignore
    getItems: function getItems() {
      return [{
        handler: 'rubric',
        data: {
          rubric: {
            rubric: {
              id: 142,
            },
          },
        },
      }];
    },
  };

  const request: IServerCtx = {
    news: {
      isRegion: true,
      regionId: 142,
      rubricsInfo: {
        regions: {
          '142': {
            alias: 'Donetsk',
            names: {
              ru: 'Донецк',
            },
            country_id: 187,
          },
        },
      },
    },
    neo: {
      platform: EPlatform.DESKTOP,
      tld: ETld.RU,
      page: EPage.RUBRIC,
      ctxRR,
      flags: {
        'yxneo_show-regions-top': '1',
      },
    },
  } as unknown as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Информация для СМИ',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
    {
      text: 'База данных СМИ',
      url: 'https://yandex.ru/news/smi',
    },
    {
      text: 'Пользовательское соглашение',
      url: 'https://yandex.ru/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      url: 'https://yandex.ru/adv/',
    },
    {
      text: 'Обратная связь',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Сейчас в СМИ. Статистика',
      url: 'https://yandex.ru/news/top',
    },
    {
      text: 'Справка',
      url: 'https://yandex.ru/support/news/',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);
});

test('getFooter by desktop', () => {
  const request: IServerCtx = {
    neo: {
      platform: EPlatform.DESKTOP,
      tld: ETld.BY,
      flags: {},
    },
  } as IServerCtx;

  const actualFooter = getFooter(request);
  const expectedFooter = [
    {
      text: 'Информация для СМИ',
      url: 'https://yandex.ru/support/news/info-for-mass-media.html',
    },
    {
      text: 'База данных СМИ',
      url: 'https://yandex.by/news/smi',
    },
    {
      text: 'Пользовательское соглашение',
      url: 'https://yandex.by/legal/news_termsofuse/',
    },
    {
      text: 'Реклама',
      url: 'https://yandex.by/adv/',
    },
    {
      text: 'Обратная связь',
      url: 'https://yandex.ru/support/news/troubleshooting/feedback.html',
    },
    {
      text: 'Справка',
      url: 'https://yandex.ru/support/news/',
    },
  ];

  expect(actualFooter).toMatchObject(expectedFooter);
});
