import { getLink } from 'sport/lib/getLink';
import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { ETarget } from 'neo/types/html';

test('getLink', () => {
  const args = {
    target: ETarget.BLANK,
    url: 'https://yandex.ru/turbo?text=https%3A//www.vesti.ru/doc.html',
  };

  const actualLink = [getLink(getServerCtxStub(), args)];
  const expectedLink = ['https://yandex.ru/turbo?text=https%3A%2F%2Fwww.vesti.ru%2Fdoc.html&utm_source=yxsport&utm_medium=mobile'];

  expect(actualLink).toMatchObject(expectedLink);
});

test('getLink internal', () => {
  const request = getServerCtxStub({
    specialArgs: {
      sport: {
        isAppSearchHeader: true,
      },
    },
  });

  const args = {
    target: ETarget.SELF,
    url: 'http://yandex.ru/sport/story/abc?text=https%3A//www.vesti.ru/doc.html?srcrwr=RENDERER:betas-3-1.myt.yp-c.yandex.net:170:50000',
    params: {
      page: '1',
    },
  };

  const actualLink = [getLink(request, args)];
  const expectedLink = ['http://yandex.ru/sport/story/abc?text=https%3A%2F%2Fwww.vesti.ru%2Fdoc.html%3Fsrcrwr%3DRENDERER%3Abetas-3-1.myt.yp-c.yandex.net%3A170%3A50000&appsearch_header=1&utm_referrer=from_sport&page=1'];

  expect(actualLink).toMatchObject(expectedLink);
});
