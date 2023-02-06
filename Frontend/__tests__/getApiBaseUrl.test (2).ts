import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { ETld } from 'neo/types/ETld';
import { IRequest } from 'mg/types/apphost';
import { getSearchApiBaseUrl, getSubscribeApiBaseUrl, getNewsApiBaseUrl } from '../getApiBaseUrl';

describe('getApiBaseUrl', () => {
  describe('getSearchApiBaseUrl', () => {
    it('проверка продовых урлов', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'yandex.ru',
            } as IRequest,
          },
        },
      });

      const searchApi = getSearchApiBaseUrl(serverCtx);

      expect(searchApi).toBe('https://yandex.ru');
    });

    it('проверка stable', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'tunneler-si.yandex.ru',
            } as IRequest,
          },
        },
      });

      const searchApi = getSearchApiBaseUrl(serverCtx);

      expect(searchApi).toBe('https://yandexsport.stable.priemka.yandex.ru');
    });

    it('проверка unstable', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'yandexsport.unstable.priemka.yandex',
            } as IRequest,
          },
        },
      });

      const searchApi = getSearchApiBaseUrl(serverCtx);

      expect(searchApi).toBe('https://yandexsport.unstable.priemka.yandex');
    });
  });

  describe('getSubscribeApiBaseUrl', () => {
    it('проверка продовых урлов', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'yandex.ru',
            } as IRequest,
          },
        },
      });

      const subscribeApi = getSubscribeApiBaseUrl(serverCtx);

      expect(subscribeApi).toBe('https://yandex.ru');
    });

    it('проверка stable', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'tunneler-si.yandex.ru',
            } as IRequest,
          },
        },
      });

      const subscribeApi = getSubscribeApiBaseUrl(serverCtx);

      expect(subscribeApi).toBe('https://yandexsport.stable.priemka.yandex.ru');
    });

    it('проверка unstable', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'yandexsport.unstable.priemka.yandex',
            } as IRequest,
          },
        },
      });

      const subscribeApi = getSubscribeApiBaseUrl(serverCtx);

      expect(subscribeApi).toBe('https://yandexsport.unstable.priemka.yandex');
    });
  });

  describe('getNewsApiBaseUrl', () => {
    it('проверка продовых урлов', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'yandex.ru',
            } as IRequest,
          },
        },
      });

      const newsApi = getNewsApiBaseUrl(serverCtx);

      expect(newsApi).toBe('https://news.yandex.ru');
    });

    it('проверка stable', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'tunneler-si.yandex.ru',
            } as IRequest,
          },
        },
      });

      const newsApi = getNewsApiBaseUrl(serverCtx);

      expect(newsApi).toBe('https://news.stable.priemka.yandex.ru');
    });

    it('проверка unstable', () => {
      const serverCtx = getServerCtxStub({
        specialArgs: {
          neo: { tld: ETld.RU },
          sport: {
            request: {
              scheme: 'https',
              hostname: 'news.unstable.priemka.yandex.ru',
            } as IRequest,
          },
        },
      });

      const newsApi = getNewsApiBaseUrl(serverCtx);

      expect(newsApi).toBe('https://news.unstable.priemka.yandex.ru');
    });
  });
});
