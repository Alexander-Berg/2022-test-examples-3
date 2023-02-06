import { checkIfTurboAndGetUrl } from 'news/lib/story';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';
import { turboUrls } from 'news/tests/stubs/contexts/item/turboUrls';

describe('checkIfTurboAndGetUrl', () => {
  it('добавляет query-параметр trbsrc для турбированных ссылок', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        news: {
          request: {
            scheme: 'https',
            hostname: 'yandex.ru',
          },
        },
      },
    });
    const { isTurbo, url } = checkIfTurboAndGetUrl(serverCtx, turboUrls, 'https://vz.ru');
    const parsedUrl = new URL(url);

    expect(isTurbo).toBe(true);
    expect(parsedUrl.searchParams.get('trbsrc')).toEqual(`neo-${serverCtx.neo.service}`);
  });

  it('не добавляет query-параметр trbsrc для нетурбированных ссылок', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        news: {
          request: {
            scheme: 'https',
            hostname: 'yandex.ru',
          },
        },
      },
    });
    const { isTurbo, url } = checkIfTurboAndGetUrl(serverCtx, turboUrls, 'https://www.starhit.ru');
    const parsedUrl = new URL(url);

    expect(isTurbo).toBe(false);
    expect(parsedUrl.searchParams.get('trbsrc')).toBeNull();
  });

  it('добавляет все значения из параметра additionalParams', () => {
    const serverCtx = getServerCtxStub({
      specialArgs: {
        news: {
          request: {
            scheme: 'https',
            hostname: 'yandex.ru',
          },
        },
      },
    });

    const { url } = checkIfTurboAndGetUrl(
      serverCtx,
      turboUrls,
      'https://www.starhit.ru',
      {
        'news-article': '1',
        utm_source: 'yandex_article',
      },
    );
    const searchParams = new URLSearchParams(new URL(url).search);
    expect(
      searchParams.get('news-article') === '1'
      && searchParams.get('utm_source') === 'yandex_article',
    ).toBeTruthy();
  });
});
