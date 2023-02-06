import { getApiBaseUrl } from 'news/lib/getApiBaseUrl';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getApiEndpoint', () => {
  // yandex.tld -> news.yandex.tld
  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'ru',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'yandex.ru',
        },
      },
    },
  }))).toEqual('https://news.yandex.ru');

  // newsssearch.yandex.tld -> news.yandex.tld
  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'by',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'newsssearch.yandex.by',
        },
      },
    },
  }))).toEqual('https://news.yandex.by');

  // (.*\.)?news.stable.priemka.yandex.tld -> (.*\.)?news.stable.priemka.yandex.tld
  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'kz',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'news.stable.priemka.yandex.kz',
        },
      },
    },
  }))).toEqual('https://news.stable.priemka.yandex.kz');

  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'ua',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'begemot-news-610-1.news.stable.priemka.yandex.ua',
        },
      },
    },
  }))).toEqual('https://begemot-news-610-1.news.stable.priemka.yandex.ua');

  // (.*\.)?news.unstable.priemka.yandex.tld -> (.*\.)?news.unstable.priemka.yandex.tld
  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'uz',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'news.unstable.priemka.yandex.uz',
        },
      },
    },
  }))).toEqual('https://news.unstable.priemka.yandex.uz');

  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'com',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'data.news.unstable.priemka.yandex.com',
        },
      },
    },
  }))).toEqual('https://data.news.unstable.priemka.yandex.com');

  // локальная разработка
  expect(getApiBaseUrl(getServerCtxStub({
    specialArgs: {
      neo: {
        tld: 'com',
      },
      news: {
        request: {
          scheme: 'https',
          hostname: 'molodorich-1-ws3.tunneler-si.yandex.com',
        },
      },
    },
  }))).toEqual('https://news.stable.priemka.yandex.com');
});
