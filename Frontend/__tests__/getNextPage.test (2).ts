import { URL } from 'url';
import { getNextPage } from 'news/lib/search/getNextPage';
import { getServerCtxStub } from 'news/tests/stubs/contexts/ServerCtx';

test('getNextPage undefined', () => {
  const request = getServerCtxStub({
    specialArgs: {
      neo: {
        url: new URL('https://yandex.ru/news/story/Sobyanin_obyavil?p=3'),
      },
    },
  });

  const actualNavigationMenu = [getNextPage(request, 4)];
  expect(actualNavigationMenu).toEqual([undefined]);
});

test('getNextPage', () => {
  const request = getServerCtxStub({
    specialArgs: {
      neo: {
        url: new URL('https://yandex.ru/news/story/Sobyanin_obyavil?p=2'),
      },
    },
  });

  const actualNavigationMenu = [getNextPage(request, 4)];
  expect(actualNavigationMenu).toEqual(['https://yandex.ru/news/story/Sobyanin_obyavil?p=3']);
});
